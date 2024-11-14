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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.ClauseNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QueryExpressionNode;
import io.ballerina.compiler.syntax.tree.SelectClauseNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.ArrayType;
import org.ballerinalang.diagramutil.connector.models.connector.types.PrimitiveType;
import org.ballerinalang.diagramutil.connector.models.connector.types.RecordType;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates types of the data mapper model.
 *
 * @since 1.4.0
 */
public class DataMapManager {

    private final WorkspaceManager workspaceManager;
    private final SemanticModel semanticModel;
    private final Document document;
    private final Gson gson;

    public DataMapManager(WorkspaceManager workspaceManager, SemanticModel semanticModel, Document document) {
        this.workspaceManager = workspaceManager;
        this.semanticModel = semanticModel;
        this.document = document;
        this.gson = new Gson();
    }

    public JsonElement getTypes(JsonElement node, String propertyKey) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        Codedata codedata = flowNode.codedata();
        NodeKind nodeKind = codedata.node();
        if (nodeKind == NodeKind.VARIABLE) {
            String dataType = flowNode.properties().get(Property.DATA_TYPE_KEY).toSourceCode();
            Optional<Symbol> varSymbol = getSymbol(semanticModel.moduleSymbols(), dataType);
            if (varSymbol.isEmpty()) {
                throw new IllegalStateException("Symbol cannot be found for : " + dataType);
            }
            Type t = Type.fromSemanticSymbol(varSymbol.get());
            if (t == null) {
                throw new IllegalStateException("Type cannot be found for : " + propertyKey);
            }
            return gson.toJsonTree(t);
        } else if (nodeKind == NodeKind.FUNCTION_CALL) {
            Optional<Symbol> varSymbol = getSymbol(semanticModel.moduleSymbols(), codedata.symbol());
            if (varSymbol.isEmpty() || varSymbol.get().kind() != SymbolKind.FUNCTION) {
                throw new IllegalStateException("Symbol cannot be found for : " + codedata.symbol());
            }
            Optional<List<ParameterSymbol>> optParams = ((FunctionSymbol) varSymbol.get()).typeDescriptor().params();
            if (optParams.isEmpty()) {
                return new JsonObject();
            }
            Optional<Type> type = optParams.flatMap(params -> params.parallelStream()
                    .filter(param -> param.nameEquals(propertyKey)).findAny()).map(Type::fromSemanticSymbol);
            if (type.isEmpty()) {
                throw new IllegalStateException("Type cannot be found for : " + propertyKey);
            }
            return gson.toJsonTree(type.get());
        }
        return new JsonObject();
    }

    private Optional<Symbol> getSymbol(List<Symbol> symbols, String name) {
        return symbols.parallelStream()
                .filter(symbol -> symbol.nameEquals(name))
                .findAny();
    }

    public JsonElement getMappings(JsonElement node, LinePosition position, String propertyKey, Path filePath,
                                   Project project) {
        // TODO: add tests for enum
        // TODO: Add array tests with where, select clauses
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, this.workspaceManager, filePath);
        Map<Path, List<TextEdit>> textEdits =
                NodeBuilder.getNodeFromKind(flowNode.codedata().node()).toSource(sourceBuilder);
        String source = textEdits.entrySet().stream().iterator().next().getValue().get(0).getNewText();
        TextDocument textDocument = document.textDocument();
        int startTextPosition = textDocument.textPositionFrom(position);
        io.ballerina.tools.text.TextEdit te = io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                0), source);
        io.ballerina.tools.text.TextEdit[] tes = {te};
        TextDocument modifiedTextDoc = textDocument.apply(TextDocumentChange.from(tes));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                modifiedTextDoc.textLines())).apply();

        SemanticModel newSemanticModel = modifiedDoc.module().packageInstance().getCompilation()
                .getSemanticModel(modifiedDoc.module().moduleId());
        LinePosition startLine = modifiedTextDoc.linePositionFrom(startTextPosition);
        LinePosition endLine = modifiedTextDoc.linePositionFrom(startTextPosition + source.length());
        Range range = new Range(new Position(startLine.line(), startLine.offset()),
                new Position(endLine.line(), endLine.offset()));
        NonTerminalNode stNode = CommonUtil.findNode(range, modifiedDoc.syntaxTree());

        List<MappingType> inputTypes = getInputTypes(newSemanticModel, modifiedDoc, position);
        inputTypes.sort(Comparator.comparing(mt -> mt.id));
        MappingType output = null;
        List<Mapping> mappings = new ArrayList<>();

        if (stNode.kind() == SyntaxKind.LOCAL_VAR_DECL) {
            Optional<Symbol> optSymbol = newSemanticModel.symbol(stNode);
            if (optSymbol.isPresent()) {
                Symbol symbol = optSymbol.get();
                if (symbol.kind() == SymbolKind.VARIABLE) {
                    Optional<String> optSymbolName = symbol.getName();
                    if (optSymbolName.isPresent()) {
                        String symbolName = optSymbolName.get();
                        Type type = Type.fromSemanticSymbol(symbol);
                        VariableDeclarationNode variableNode = (VariableDeclarationNode) stNode;
                        if (type.getTypeName().equals("record")) {
                            output = getMappingType(symbolName, type);
                            generateRecordVariableDataMapping(variableNode, mappings, symbolName, newSemanticModel);
                        } else if (type.getTypeName().equals("array")) {
                            output = getMappingType(symbolName, type);
                            generateArrayVariableDataMapping(variableNode, mappings, symbolName, newSemanticModel);
                        }
                    }
                }
            }
        }
        return gson.toJsonTree(new Model(inputTypes, output, mappings, source, "root"));
    }

    private void generateRecordVariableDataMapping(VariableDeclarationNode varDecl, List<Mapping> mappings,
                                                   String name, SemanticModel semanticModel) {
        Optional<ExpressionNode> optInitializer = varDecl.initializer();
        if (optInitializer.isEmpty()) {
            return;
        }
        ExpressionNode expressionNode = optInitializer.get();
        SyntaxKind exprKind = expressionNode.kind();
        if (exprKind == SyntaxKind.MAPPING_CONSTRUCTOR) {
            genMapping((MappingConstructorExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            genMapping((SimpleNameReferenceNode) expressionNode, mappings, name, semanticModel);
        }
    }

    private void generateArrayVariableDataMapping(VariableDeclarationNode varDecl, List<Mapping> mappings,
                                                  String name, SemanticModel semanticModel) {
        Optional<ExpressionNode> optInitializer = varDecl.initializer();
        if (optInitializer.isEmpty()) {
            return;
        }
        ExpressionNode expressionNode = optInitializer.get();
        SyntaxKind exprKind = expressionNode.kind();
        if (exprKind == SyntaxKind.LIST_CONSTRUCTOR) {
            genMapping((ListConstructorExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.QUERY_EXPRESSION) {
            genMapping((QueryExpressionNode) expressionNode, mappings, name, semanticModel);
        } else if (exprKind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            genMapping((SimpleNameReferenceNode) expressionNode, mappings, name, semanticModel);
        }

    }

    private void genMapping(MappingConstructorExpressionNode mappingCtrExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        for (MappingFieldNode field : mappingCtrExpr.fields()) {
            if (field.kind() == SyntaxKind.SPECIFIC_FIELD) {
                SpecificFieldNode f = (SpecificFieldNode) field;
                Optional<ExpressionNode> optFieldExpr = f.valueExpr();
                if (optFieldExpr.isEmpty()) {
                    continue;
                }
                ExpressionNode fieldExpr = optFieldExpr.get();
                SyntaxKind kind = fieldExpr.kind();
                if (kind == SyntaxKind.MAPPING_CONSTRUCTOR) {
                    genMapping((MappingConstructorExpressionNode) fieldExpr, mappings, name + "." + f.fieldName(),
                            semanticModel);
                } else if (kind == SyntaxKind.LIST_CONSTRUCTOR) {
                    genMapping((ListConstructorExpressionNode) fieldExpr, mappings, name + "." + f.fieldName(),
                            semanticModel);
                } else {
                    List<String> inputs = new ArrayList<>();
                    genInputs(fieldExpr, inputs);
                    Mapping mapping = new Mapping(name + "." + f.fieldName(), inputs,
                            fieldExpr.toSourceCode(), getDiagnostics(fieldExpr.lineRange(), semanticModel));
                    mappings.add(mapping);
                }
            }
        }
    }

    private void genMapping(SimpleNameReferenceNode varRef, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        List<String> inputs = new ArrayList<>();
        genInputs(varRef, inputs);
        Mapping mapping = new Mapping(name, inputs, varRef.toSourceCode(),
                getDiagnostics(varRef.lineRange(), semanticModel));
        mappings.add(mapping);
    }

    private void genMapping(ListConstructorExpressionNode listCtrExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        SeparatedNodeList<Node> expressions = listCtrExpr.expressions();
        int size = expressions.size();
        for (int i = 0; i < size; i++) {
            Node expr = expressions.get(i);
            if (expr.kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
                continue;
            }
            genMapping((MappingConstructorExpressionNode) expr, mappings, name + "." + i, semanticModel);
        }
    }

    private void genMapping(QueryExpressionNode queryExpr, List<Mapping> mappings, String name,
                            SemanticModel semanticModel) {
        // ((SelectClauseNode) expressionNode.resultClause()).expression()
        ClauseNode clauseNode = queryExpr.resultClause();
        if (clauseNode.kind() != SyntaxKind.SELECT_CLAUSE) {
            return;
        }
        SelectClauseNode selectClauseNode = (SelectClauseNode) clauseNode;
        ExpressionNode expr = selectClauseNode.expression();
        if (expr.kind() == SyntaxKind.MAPPING_CONSTRUCTOR) {
            genMapping((MappingConstructorExpressionNode) expr, mappings, name, semanticModel);
        }
    }

    private void genInputs(Node expr, List<String> inputs) {
        SyntaxKind kind = expr.kind();
        if (kind == SyntaxKind.FIELD_ACCESS) {
            String source = expr.toSourceCode().trim();
            String[] split = source.split("\\[");
            if (split.length > 1) {
                inputs.add(split[0]);
            } else {
                inputs.add(source);
            }
        } else if (kind == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            inputs.add(expr.toSourceCode().trim());
        } else if (kind == SyntaxKind.BINARY_EXPRESSION) {
            BinaryExpressionNode binaryExpr = (BinaryExpressionNode) expr;
            genInputs(binaryExpr.lhsExpr(), inputs);
            genInputs(binaryExpr.rhsExpr(), inputs);
        } else if (kind == SyntaxKind.METHOD_CALL) {
            MethodCallExpressionNode methodCallExpr = (MethodCallExpressionNode) expr;
            genInputs(methodCallExpr.expression(), inputs);
        } else if (kind == SyntaxKind.MAPPING_CONSTRUCTOR) {
            MappingConstructorExpressionNode mappingCtrExpr = (MappingConstructorExpressionNode) expr;
            for (MappingFieldNode field : mappingCtrExpr.fields()) {
                SyntaxKind fieldKind = field.kind();
                if (fieldKind == SyntaxKind.SPECIFIC_FIELD) {
                    Optional<ExpressionNode> optFieldExpr = ((SpecificFieldNode) field).valueExpr();
                    optFieldExpr.ifPresent(expressionNode -> genInputs(expressionNode, inputs));
                } else {
                    genInputs(field, inputs);
                }
            }
        }
    }

    private List<String> getDiagnostics(LineRange lineRange, SemanticModel semanticModel) {
        List<Diagnostic> diagnostics = semanticModel.diagnostics(lineRange);
        List<String> diagnosticMsgs = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            diagnosticMsgs.add(diagnostic.message());
        }
        return diagnosticMsgs;
    }

    private List<MappingType> getInputTypes(SemanticModel semanticModel, Document document, LinePosition position) {
        List<MappingType> mappingTypes = new ArrayList<>();

        List<Symbol> symbols = semanticModel.visibleSymbols(document, position);
        for (Symbol symbol : symbols) {
            SymbolKind kind = symbol.kind();
            if (kind == SymbolKind.VARIABLE) {
                Optional<String> optName = symbol.getName();
                if (optName.isEmpty()) {
                    continue;
                }
                Type type = Type.fromSemanticSymbol(symbol);
                MappingType mappingType = getMappingType(optName.get(), type);
                if (mappingType == null) {
                    continue;
                }
                VariableSymbol varSymbol = (VariableSymbol) symbol;
                if (varSymbol.qualifiers().contains(Qualifier.CONFIGURABLE)) {
                    mappingType.category = "configurable";
                } else {
                    mappingType.category = "variable";
                }
                mappingTypes.add(mappingType);
            } else if (kind == SymbolKind.CONSTANT) {
                Type type = Type.fromSemanticSymbol(symbol);
                // TODO: Name of constant is set to type name, check that
                MappingType mappingType = getMappingType(type.getTypeName(), type);
                if (mappingType == null) {
                    continue;
                }
                mappingType.category = "constant";
                mappingTypes.add(mappingType);
            }
        }
        return mappingTypes;
    }

    private MappingType getMappingType(String name, Type type) {
        if (type.getTypeName().equals("record")) {
            RecordType recordType = (RecordType) type;
            MappingRecordType mappingRecordType = new MappingRecordType(name, type);
            for (Type field : recordType.fields) {
                mappingRecordType.fields.add(getMappingType(name + "." + field.getName(), field));
            }
            return mappingRecordType;
        } else if (type instanceof PrimitiveType) {
            return new MappingType(name, type);
        } else if (type.getTypeName().equals("array")) {
            ArrayType arrayType = (ArrayType) type;
            MappingArrayType mappingArrayType = new MappingArrayType(name, type);
            mappingArrayType.member = getMappingType(name, arrayType.memberType);
            return mappingArrayType;
        } else {
            return null;
        }
    }

    private static final java.lang.reflect.Type mt = new TypeToken<List<Mapping>>() {
    }.getType();

    public String getSource(JsonElement mp, JsonElement fNode) {
        FlowNode flowNode = gson.fromJson(fNode, FlowNode.class);
        List<Mapping> fieldMapping = gson.fromJson(mp, mt);
        Map<String, Object> mappings = new HashMap<>(); // TODO: Maintain the order of fields
        for (Mapping mapping : fieldMapping) {
            genSourceForMapping(mapping, mappings);
        }
        String mappingSource = genSource(mappings);
        if (flowNode.codedata().node() == NodeKind.VARIABLE) {
            Optional<Property> optProperty = flowNode.getProperty("expression");
            if (optProperty.isPresent()) {
                Property property = optProperty.get();
                String source = property.toSourceCode();
                if (source.matches("^from.*in.*select.*$")) {
                    String[] split = source.split("select");
                    return split[0] + " select " + mappingSource + ";";
                }
            }
        }

        return mappingSource;
    }

    private String genSource(Map<String, Object> mappings) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        int len = mappings.entrySet().size();
        int i = 0;
        for (Map.Entry<String, Object> stringObjectEntry : mappings.entrySet()) {
            sb.append(stringObjectEntry.getKey()).append(":");
            if (stringObjectEntry.getValue() instanceof Map<?, ?>) {
                sb.append(genSource((Map<String, Object>) stringObjectEntry.getValue()));
            } else {
                sb.append(stringObjectEntry.getValue());
            }
            if (i != len - 1) {
                sb.append(",");
            }
            i = i + 1;
        }
        sb.append("}");
        return sb.toString();
    }

    private void genSourceForMapping(Mapping mapping, Map<String, Object> mappingSource) {
        String output = mapping.output;
        String[] splits = output.split("\\.");
        if (splits.length == 1) {
            return;
        }

        Map<String, Object> currentMapping = mappingSource;
        for (int i = 1; i < splits.length - 1; i++) {
            Object o = currentMapping.get(splits[i]);
            if (o == null) {
                Map<String, Object> newMapping = new HashMap<>();
                currentMapping.put(splits[i], newMapping);
                currentMapping = newMapping;
            } else if (o instanceof Map<?, ?>) {
                currentMapping = (Map<String, Object>) o;
            }
        }
        currentMapping.put(splits[splits.length - 1], mapping.expression);
    }

    private record Model(List<MappingType> inputs, MappingType output, List<Mapping> mappings, String source,
                         String view) {

    }

    private record Mapping(String output, List<String> inputs, String expression, List<String> diagnostics) {

    }

    // TODO: Recheck the constructor generation
    private static class MappingType {
        String id;
        String category;
        Type type;

        MappingType(String id, Type type) {
            this.id = id;
            this.type = type;
        }

        String getCategory() {
            return this.category;
        }

        Type getType() {
            return this.type;
        }
    }

    private static class MappingRecordType extends MappingType {
        List<MappingType> fields = new ArrayList<>();

        MappingRecordType(String id, Type type) {
            super(id, type);
        }
    }

    private static class MappingArrayType extends MappingType {
        MappingType member;

        MappingArrayType(String id, Type type) {
            super(id, type);
        }
    }
}
