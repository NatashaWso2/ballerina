/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.plugins.idea.psi.scopeprocessors;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import org.ballerinalang.plugins.idea.completion.BallerinaCompletionUtils;
import org.ballerinalang.plugins.idea.psi.BallerinaCatchClause;
import org.ballerinalang.plugins.idea.psi.BallerinaExpression;
import org.ballerinalang.plugins.idea.psi.BallerinaFieldDefinition;
import org.ballerinalang.plugins.idea.psi.BallerinaFieldDefinitionList;
import org.ballerinalang.plugins.idea.psi.BallerinaNamedPattern;
import org.ballerinalang.plugins.idea.psi.BallerinaRecordKey;
import org.ballerinalang.plugins.idea.psi.BallerinaRecordKeyValue;
import org.ballerinalang.plugins.idea.psi.BallerinaRecordLiteralExpression;
import org.ballerinalang.plugins.idea.psi.BallerinaRecordTypeName;
import org.ballerinalang.plugins.idea.psi.BallerinaStatement;
import org.ballerinalang.plugins.idea.psi.BallerinaTypeDefinition;
import org.ballerinalang.plugins.idea.psi.BallerinaTypeName;
import org.ballerinalang.plugins.idea.psi.BallerinaVariableDefinitionStatement;
import org.ballerinalang.plugins.idea.psi.impl.BallerinaPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Responsible for resolving and completing definitions in statements.
 */
public class BallerinaStatementProcessor extends BallerinaScopeProcessorBase {

    @Nullable
    private final CompletionResultSet myResult;
    @NotNull
    private final PsiElement myElement;

    public BallerinaStatementProcessor(@Nullable CompletionResultSet result, @NotNull PsiElement element,
                                       boolean isCompletion) {
        super(element, element, isCompletion);
        myResult = result;
        myElement = element;
    }

    protected boolean accept(@NotNull PsiElement element) {
        return element instanceof BallerinaStatement;
    }

    @Override
    public boolean execute(@NotNull PsiElement scopeElement, @NotNull ResolveState state) {
        ProgressManager.checkCanceled();
        if (accept(scopeElement)) {
            BallerinaStatement statement = (BallerinaStatement) scopeElement;

            // This is to suggest fields in when the caret is in a record literal.
            BallerinaRecordKey ballerinaRecordKey = PsiTreeUtil.getParentOfType(myElement, BallerinaRecordKey.class);
            // Todo - Check for record literal expression to find other types.
            if (ballerinaRecordKey != null) {

                BallerinaRecordLiteralExpression literalExpression = PsiTreeUtil.getParentOfType(ballerinaRecordKey,
                        BallerinaRecordLiteralExpression.class);
                if (literalExpression != null && literalExpression.getParent() instanceof BallerinaRecordKeyValue) {

                    BallerinaRecordKeyValue expressionParent = (BallerinaRecordKeyValue) literalExpression.getParent();
                    BallerinaRecordKey parentRecordKey = expressionParent.getRecordKey();


                    BallerinaExpression expression = parentRecordKey.getExpression();
                    if (expression == null) {
                        return true;
                    }
                    PsiElement resolvedElement = BallerinaPsiImplUtil.getBallerinaTypeFromExpression(expression);
                    if (resolvedElement != null) {
                        PsiElement parent = resolvedElement.getParent();
                        if (resolvedElement instanceof BallerinaRecordTypeName) {
                            BallerinaFieldDefinitionList fieldDefinitionList = PsiTreeUtil.findChildOfType(parent,
                                    BallerinaFieldDefinitionList.class);
                            List<BallerinaFieldDefinition> fieldDefinitions =
                                    PsiTreeUtil.getChildrenOfTypeAsList(fieldDefinitionList,
                                            BallerinaFieldDefinition.class);
                            for (BallerinaFieldDefinition ballerinaFieldDefinition : fieldDefinitions) {
                                PsiElement identifier = ballerinaFieldDefinition.getIdentifier();
                                if (myResult != null) {
                                    BallerinaTypeName fieldTypeName = ballerinaFieldDefinition.getTypeName();
                                    String type;
                                    if (fieldTypeName instanceof BallerinaRecordTypeName) {
                                        type = "record {}";
                                    } else {
                                        type = fieldTypeName.getText();
                                    }
                                    myResult.addElement(BallerinaCompletionUtils.createFieldLookupElement(identifier,
                                            resolvedElement, type,
                                            BallerinaPsiImplUtil.getObjectFieldDefaultValue(ballerinaFieldDefinition),
                                            null, false));
                                } else if (myElement.getText().equals(identifier.getText())) {
                                    add(identifier);
                                }
                            }

                        } else if (parent instanceof BallerinaTypeDefinition) {
                            BallerinaObjectFieldProcessor ballerinaFieldProcessor = new BallerinaObjectFieldProcessor
                                    (myResult, myElement, isCompletion());
                            ballerinaFieldProcessor.execute(parent, ResolveState.initial());
                            PsiElement result = ballerinaFieldProcessor.getResult();
                            if (!isCompletion() && result != null) {
                                add(result);
                                return false;
                            }
                        }
                    }
                    return false;

                }

                if (statement.getFirstChild() instanceof BallerinaVariableDefinitionStatement) {
                    PsiElement resolvedElement = BallerinaPsiImplUtil.resolveBallerinaType((
                            (BallerinaVariableDefinitionStatement) statement.getFirstChild()));
                    if (resolvedElement != null && resolvedElement.getParent() instanceof BallerinaTypeDefinition) {
                        BallerinaObjectFieldProcessor ballerinaFieldProcessor = new BallerinaObjectFieldProcessor
                                (myResult, myElement, isCompletion());
                        ballerinaFieldProcessor.execute(resolvedElement.getParent(), ResolveState.initial());
                        PsiElement result = ballerinaFieldProcessor.getResult();
                        if (!isCompletion() && result != null) {
                            add(result);
                            return false;
                        }
                    }
                }
                // We don't want to suggest any other completion since we are currently at the key of a record literal.
                return false;
            }

            BallerinaNamedPattern ballerinaNamedPattern = PsiTreeUtil.getParentOfType(statement,
                    BallerinaNamedPattern.class);
            while (ballerinaNamedPattern != null) {
                PsiElement identifier = ballerinaNamedPattern.getIdentifier();
                if (myResult != null) {
                    myResult.addElement(BallerinaCompletionUtils.createVariableLookupElement(identifier,
                            BallerinaPsiImplUtil.formatBallerinaTypeName(ballerinaNamedPattern.getTypeName())));
                } else if (myElement.getText().equals(identifier.getText())) {
                    add(identifier);
                }
                if (!isCompletion() && getResult() != null) {
                    return false;
                }
                ballerinaNamedPattern = PsiTreeUtil.getParentOfType(ballerinaNamedPattern, BallerinaNamedPattern.class);
            }

            BallerinaCatchClause ballerinaCatchClause = PsiTreeUtil.getParentOfType(statement,
                    BallerinaCatchClause.class);
            while (ballerinaCatchClause != null) {
                PsiElement identifier = ballerinaCatchClause.getIdentifier();
                if (identifier != null) {
                    if (myResult != null) {
                        myResult.addElement(BallerinaCompletionUtils.createVariableLookupElement(identifier,
                                BallerinaPsiImplUtil.formatBallerinaTypeName(ballerinaCatchClause.getTypeName())));
                    } else if (myElement.getText().equals(identifier.getText())) {
                        add(identifier);
                    }
                }
                if (!isCompletion() && getResult() != null) {
                    return false;
                }
                ballerinaCatchClause = PsiTreeUtil.getParentOfType(ballerinaCatchClause, BallerinaCatchClause.class);
            }
        }
        return true;
    }

    @Override
    public boolean isCompletion() {
        return myIsCompletion;
    }

    @Override
    protected boolean crossOff(@NotNull PsiElement e) {
        return false;
    }
}
