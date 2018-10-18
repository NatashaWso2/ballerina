/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.model.tree.statements;

import org.ballerinalang.model.tree.IdentifierNode;
import org.ballerinalang.model.tree.expressions.ExpressionNode;

/**
 * a,d -&gt; w1.
 *
 * @since 0.94
 */
public interface WorkerSendNode extends StatementNode {

    ExpressionNode getExpression();

    ExpressionNode getKeyExpression();

    IdentifierNode getWorkerName();

    void setWorkerName(IdentifierNode identifierNode);

    boolean isForkJoinedSend();
}
