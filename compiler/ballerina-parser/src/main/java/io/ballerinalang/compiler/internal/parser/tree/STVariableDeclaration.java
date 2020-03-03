/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package io.ballerinalang.compiler.internal.parser.tree;

import io.ballerinalang.compiler.syntax.tree.LocalVariableDeclaration;
import io.ballerinalang.compiler.syntax.tree.Node;
import io.ballerinalang.compiler.syntax.tree.NonTerminalNode;

public class STVariableDeclaration extends STStatement {

    public final STNode typeName;
    public final STNode variableName;
    public final STNode equalsToken;
    public final STNode initializer;
    public final STNode semicolonToken;

    public STVariableDeclaration(SyntaxKind kind,
                                 STNode typeName,
                                 STNode variableName,
                                 STNode equalsToken,
                                 STNode initializer,
                                 STNode semicolonToken) {
        super(kind);
        this.typeName = typeName;
        this.variableName = variableName;
        this.equalsToken = equalsToken;
        this.initializer = initializer;
        this.semicolonToken = semicolonToken;

        this.bucketCount = 5;
        this.childBuckets = new STNode[this.bucketCount];
        this.addChildNode(typeName, 0);
        this.addChildNode(variableName, 1);
        this.addChildNode(equalsToken, 2);
        this.addChildNode(initializer, 3);
        this.addChildNode(semicolonToken, 4);
    }

    @Override
    public Node createFacade(int position, NonTerminalNode parent) {
        return new LocalVariableDeclaration(this, position, parent);
    }
}
