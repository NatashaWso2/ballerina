/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.testerina.test;

import org.ballerinalang.testerina.core.BTestRunner;
import org.ballerinalang.testerina.core.TesterinaRegistry;
import org.ballerinalang.testerina.util.Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.util.ProjectDirConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is responsible of testing the ballerina samples
 */
public class TesterinaSamplesTest {

    private String testerinaRoot;

    @BeforeClass
    public void setUserDir() throws IOException {
        testerinaRoot = System.getProperty("user.dir") + "/../../samples";
        Path filePath = Paths.get(testerinaRoot, ProjectDirConstants.MANIFEST_FILE_NAME);
        Utils.cleanUpDir(filePath);
        Files.createFile(filePath);
    }

    // /samples/functionTest
    @Test
    public void functionTestSampleTest() {
        TesterinaRegistry.getInstance().setOrgName("$anon");
        BTestRunner runner = new BTestRunner();
        runner.runTest(testerinaRoot, new Path[] { Paths.get("functionTest") }, new ArrayList<>());
        Assert.assertEquals(runner.getTesterinaReport().getTestSummary("functionTest", "passed"), 6);
        // Reset the org name
    }

    // /samples/features/assertions.bal
    @Test
    public void assertSampleTest() {
        BTestRunner runner = new BTestRunner();
        runner.runTest(testerinaRoot + "/features/", new Path[]{Paths.get("assertions.bal")}, new ArrayList<>());
        Assert.assertEquals(runner.getTesterinaReport().getTestSummary(".", "passed"), 14);
    }

    // /samples/features/assertions.bal
    @Test
    public void dataProviderSampleTest() {
        BTestRunner runner = new BTestRunner();
        runner.runTest(testerinaRoot + "/features/", new Path[]{Paths.get("data-providers.bal")}, new ArrayList<>());
        Assert.assertEquals(runner.getTesterinaReport().getTestSummary(".", "passed"), 4);
    }

    @AfterMethod
    private void cleanup() {
        TesterinaRegistry.getInstance().setOrgName(null);
        TesterinaRegistry.getInstance().setProgramFiles(new ArrayList<>());
        TesterinaRegistry.getInstance().setTestSuites(new HashMap<>());
        TesterinaRegistry.getInstance().getInitializedPackages().clear();
    }

    @AfterClass
    public void cleanDirectory() throws IOException {
        Utils.cleanUpDir(Paths.get(testerinaRoot,  ProjectDirConstants.MANIFEST_FILE_NAME));
    }

}
