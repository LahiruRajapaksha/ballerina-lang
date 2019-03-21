/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.langserver.completions.resolvers;

import org.antlr.v4.runtime.ParserRuleContext;
import org.ballerinalang.langserver.common.UtilSymbolKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.util.CompletionItemResolver;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;

import java.util.List;

/**
 * ServiceContextResolver.
 */
public class ServiceContext extends AbstractItemContext implements CompletionItemsContext {
    @Override
    public CompletionItemsContext resolve(LSContext ctx) {
        ParserRuleContext parserRuleContext = ctx.get(CompletionKeys.PARSER_RULE_CONTEXT_KEY);
        List<String> poppedTokens = CommonUtil.getPoppedTokenStrings(ctx);
        if (isAnnotationStart(ctx)) {
            return CompletionItemResolver.get(BallerinaParser.AnnotationAttachmentContext.class, ctx);
        } else if ((parserRuleContext == null
                || parserRuleContext instanceof BallerinaParser.ObjectFieldDefinitionContext)
                && poppedTokens.contains(UtilSymbolKeys.EQUAL_SYMBOL_KEY)) {
            return CompletionItemResolver.get(BallerinaParser.VariableDefinitionStatementContext.class, ctx);
        }
        return this;
    }
}
