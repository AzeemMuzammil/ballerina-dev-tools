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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of a transaction node in the flow model.
 *
 * @since 2.0.0
 */
public class TransactionBuilder extends NodeBuilder {

    public static final String LABEL = "Transaction";
    public static final String DESCRIPTION = "Handle transaction.";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.TRANSACTION);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Branch> body = sourceBuilder.flowNode.getBranch(Branch.BODY_LABEL);

        if (sourceBuilder.flowNode.properties() != null) {
            Property retryCount = sourceBuilder.flowNode.properties().get(Property.RETRY_COUNT_KEY);
            return sourceBuilder.token()
                    .keyword(SyntaxKind.RETRY_KEYWORD)
                    .openParen()
                    .expression(retryCount)
                    .closeParen()
                    .whiteSpace()
                    .keyword(SyntaxKind.TRANSACTION_KEYWORD)
                    .stepOut()
                    .body(body.isPresent() ? body.get().children() : Collections.emptyList())
                    .onFailure()
                    .textEdit(false)
                    .build();
        }

        return sourceBuilder.token()
                .keyword(SyntaxKind.TRANSACTION_KEYWORD)
                .stepOut()
                .body(body.isPresent() ? body.get().children() : Collections.emptyList())
                .onFailure()
                .textEdit(false)
                .build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        this.branches = List.of(Branch.DEFAULT_BODY_BRANCH, Branch.getDefaultOnFailBranch(true));
        properties().retryCount(3, true);
    }
}
