/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for handling parameters of functions and methods.
 *
 * @since 2.0.0
 */
public class ParamUtils {

    /**
     * Builds the resource path template for the given function symbol.
     *
     * @param functionSymbol the function symbol
     * @return the resource path template
     */
    public static String buildResourcePathTemplate(FunctionSymbol functionSymbol) {
        StringBuilder pathBuilder = new StringBuilder();
        ResourceMethodSymbol resourceMethodSymbol = (ResourceMethodSymbol) functionSymbol;
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                for (Symbol pathSegment : pathSegmentList.list()) {
                    pathBuilder.append("/");
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        String value = DefaultValueGeneratorUtil
                                .getDefaultValueForType(pathParameterSymbol.typeDescriptor());
                        pathBuilder.append("[").append(value).append("]");
                    } else {
                        pathBuilder.append(pathSegment.getName().orElse(""));
                    }
                }
                ((PathSegmentList) resourcePath).pathRestParameter().ifPresent(pathRestParameter -> {
                    pathBuilder.append("/path/to/subdirectory");
                });
            }
            case PATH_REST_PARAM -> pathBuilder.append("/path/to/subdirectory");
            case DOT_RESOURCE_PATH -> pathBuilder.append("\\.");
        }
        return pathBuilder.toString();
    }


    /**
     * Removes the leading single quote from the input string if it exists.
     *
     * @param input the input string
     * @return the modified string with the leading single quote removed
     */
    public static String removeLeadingSingleQuote(String input) {
        if (input != null && input.startsWith("'")) {
            return input.substring(1);
        }
        return input;
    }

    public static LinkedHashMap<String, ParameterResult> buildFunctionParamResultMap(FunctionSymbol functionSymbol,
                                                                                     SemanticModel semanticModel) {
        ParamForTypeInfer paramForTypeInfer = null;
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        if (functionSymbol.external()) {
            List<String> paramNameList = new ArrayList<>();
            functionTypeSymbol.params().ifPresent(paramList -> paramList
                    .stream()
                    .filter(paramSym -> paramSym.paramKind() == ParameterKind.DEFAULTABLE)
                    .forEach(paramSymbol -> paramNameList.add(paramSymbol.getName().orElse(""))));

            Map<String, TypeSymbol> returnTypeMap =
                    allMembers(functionTypeSymbol.returnTypeDescriptor().orElse(null));
            for (String paramName : paramNameList) {
                if (returnTypeMap.containsKey(paramName)) {
                    String defaultValue = DefaultValueGeneratorUtil
                            .getDefaultValueForType(returnTypeMap.get(paramName));
                    paramForTypeInfer = new ParamForTypeInfer(paramName, defaultValue, "json");
                    break;
                }
            }
        }
        final ParamForTypeInfer finalParamForTypeInfer = paramForTypeInfer;
        Map<String, String> documentationMap =
                functionSymbol.documentation().map(Documentation::parameterMap).orElse(Map.of());
        LinkedHashMap<String, ParameterResult> funcParamMap = new LinkedHashMap<>();
        functionTypeSymbol.params().ifPresent(paramList -> paramList.forEach(paramSymbol ->
                buildFunctionParamMap(paramSymbol, documentationMap, semanticModel, funcParamMap,
                        finalParamForTypeInfer)));
        functionTypeSymbol.restParam().ifPresent(paramSymbol ->
                buildFunctionParamMap(paramSymbol, documentationMap, semanticModel, funcParamMap, null));
        return funcParamMap;
    }

    private static Map<String, TypeSymbol> allMembers(TypeSymbol typeSymbol) {
        Map<String, TypeSymbol> members = new HashMap<>();
        if (typeSymbol == null) {
            return members;
        } else if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            unionTypeSymbol.memberTypeDescriptors()
                    .forEach(memberType -> members.put(memberType.getName().orElse(""), memberType));
        } else if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
            intersectionTypeSymbol.memberTypeDescriptors()
                    .forEach(memberType -> members.put(memberType.getName().orElse(""), memberType));
        } else {
            members.put(typeSymbol.getName().orElse(""), typeSymbol);
        }
        return members;
    }

    private static void buildIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                  SemanticModel semanticModel, ModuleInfo moduleInfo,
                                                  LinkedHashMap<String, ParameterResult> funcParamMap) {
        recordTypeSymbol.typeInclusions().forEach(includedType ->
                buildIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(includedType),
                        semanticModel, moduleInfo, funcParamMap));

        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol fieldType = CommonUtil.getRawType(recordFieldSymbol.typeDescriptor());
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
            String paramDescription = entry.getValue().documentation()
                    .flatMap(Documentation::description).orElse("");
            String paramType = CommonUtils.getTypeSignature(semanticModel, recordFieldSymbol.typeDescriptor(),
                    true, moduleInfo);
            int optional = 0;
            if (recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue()) {
                optional = 1;
            }
            funcParamMap.put(paramName, new ParameterResult(0, paramName, paramType,
                    Parameter.Kind.INCLUDED_FIELD, defaultValue, paramDescription, optional));
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleInfo);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            funcParamMap.put(Parameter.Kind.INCLUDED_RECORD_REST.name(), new ParameterResult(0,
                    "Additional Values", paramType,
                    Parameter.Kind.INCLUDED_RECORD_REST, defaultValue, "Capture key value pairs", 1));
        });
    }

    private static void buildFunctionParamMap(ParameterSymbol paramSymbol,
                                              Map<String, String> documentationMap,
                                              SemanticModel semanticModel,
                                              LinkedHashMap<String, ParameterResult> funcParamMap,
                                              ParamForTypeInfer paramForTypeInfer) {
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterKind parameterKind = paramSymbol.paramKind();
        String paramType;
        int optional = 1;
        String defaultValue;
        Parameter.Kind kind;
        ModuleInfo moduleInfo = ModuleInfo.from(paramSymbol.getModule().get().id());
        if (parameterKind == ParameterKind.REST) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) paramSymbol.typeDescriptor()).memberTypeDescriptor());
            paramType = CommonUtils.getTypeSignature(semanticModel,
                    ((ArrayTypeSymbol) paramSymbol.typeDescriptor()).memberTypeDescriptor(),
                    true, moduleInfo);
            kind = Parameter.Kind.REST_PARAMETER;
        } else if (parameterKind == ParameterKind.INCLUDED_RECORD) {
            paramType = CommonUtils.getTypeSignature(semanticModel, paramSymbol.typeDescriptor(), true,
                    moduleInfo);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            kind = Parameter.Kind.INCLUDED_RECORD;
            buildIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(paramSymbol.typeDescriptor()),
                    semanticModel, moduleInfo, funcParamMap);
        } else if (parameterKind == ParameterKind.REQUIRED) {
            paramType = CommonUtils.getTypeSignature(semanticModel, paramSymbol.typeDescriptor(), true, moduleInfo);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            optional = 0;
            kind = Parameter.Kind.REQUIRED;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    funcParamMap.put(paramName, new ParameterResult(0, paramName, paramType,
                            Parameter.Kind.PARAM_FOR_TYPE_INFER, defaultValue, paramDescription, 1));
                    return;
                }
            }
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            paramType = CommonUtils.getTypeSignature(semanticModel, paramSymbol.typeDescriptor(), true,
                    moduleInfo);
            kind = Parameter.Kind.DEFAULTABLE;
        }
        funcParamMap.put(paramName, new ParameterResult(0, paramName, paramType, kind,
                defaultValue, paramDescription, optional));
    }

    public record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }
}
