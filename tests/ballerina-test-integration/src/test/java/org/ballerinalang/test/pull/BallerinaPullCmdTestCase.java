/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.test.pull;

import org.ballerinalang.test.IntegrationTestCase;
import org.ballerinalang.test.context.BallerinaTestException;
import org.ballerinalang.test.context.Constant;
import org.ballerinalang.test.context.ServerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Testing ballerina pull command
 */
public class BallerinaPullCmdTestCase extends IntegrationTestCase {

    private static final Logger log = LoggerFactory.getLogger(BallerinaPullCmdTestCase.class);
    private ServerInstance ballerinaServer;
    private ServerInstance ballerinaClient;
    private String serverZipPath;

    @Test
    public void setUp() throws BallerinaTestException {
        String serverBal = new File(
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "ballerinaPull"
                        + File.separator + "pullServer.bal").getAbsolutePath();
        ballerinaServer = ServerInstance.initBallerinaServer();
        ballerinaServer.startBallerinaServer(serverBal);
    }

    @Test(description = "Test ballerina pull cmd")
    public void testMutualSSL() throws Exception {
        serverZipPath = System.getProperty(Constant.SYSTEM_PROP_SERVER_ZIP);
        String[] clientArgs = {new File("src" + File.separator + "test" + File.separator + "resources"
                + File.separator + "ballerinaPull" + File.separator + "pullClient.bal").getAbsolutePath()};
        ballerinaClient = new ServerInstance(serverZipPath);
        ballerinaClient.runMain(clientArgs);
    }

    @AfterClass
    private void cleanup() throws Exception {
        ballerinaServer.stopServer();
        ballerinaClient.stopServer();
    }
}
