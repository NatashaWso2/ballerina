/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.testerina.core;

import org.ballerinalang.compiler.CompilerPhase;
import org.ballerinalang.compiler.plugins.CompilerPlugin;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BIterator;
import org.ballerinalang.model.values.BJSON;
import org.ballerinalang.model.values.BNewArray;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.testerina.core.entity.TestSuite;
import org.ballerinalang.testerina.core.entity.TesterinaReport;
import org.ballerinalang.testerina.core.entity.TesterinaResult;
import org.ballerinalang.testerina.util.Utils;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.debugger.Debugger;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.Names;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * BTestRunner entity class.
 */
public class BTestRunner {

    private static PrintStream errStream = System.err;
    private static PrintStream outStream = System.out;
    private TesterinaReport tReport = new TesterinaReport();
    private TesterinaRegistry registry = TesterinaRegistry.getInstance();

    /**
     * Executes a given set of ballerina program files.
     *
     * @param sourceRoot      source root
     * @param sourceFilePaths List of @{@link Path} of ballerina files
     * @param groups          List of groups to be included
     */
    public void runTest(String sourceRoot, Path[] sourceFilePaths, List<String> groups, boolean buildWithTests) {
        runTest(sourceRoot, sourceFilePaths, groups, true, buildWithTests);
    }

    /**
     * Executes a given set of ballerina program files.
     *
     * @param sourceRoot      source root
     * @param sourceFilePaths List of @{@link Path} of ballerina files
     * @param groups          List of groups to be included/excluded
     * @param shouldIncludeGroups    flag to specify whether to include or exclude provided groups
     */
    public void runTest(String sourceRoot, Path[] sourceFilePaths, List<String> groups, boolean shouldIncludeGroups,
                        boolean buildWithTests) {
        registry.setGroups(groups);
        registry.setShouldIncludeGroups(shouldIncludeGroups);
        compileAndBuildSuites(sourceRoot, sourceFilePaths, buildWithTests);
        // execute the test programs
        execute();
        // print the report
        // tReport.printSummary();
    }

    /**
     * lists the groups available in tests.
     *
     * @param sourceRoot      source root of the project
     * @param sourceFilePaths package or program file paths
     */
    public void listGroups(String sourceRoot, Path[] sourceFilePaths) {
        //Build the test suites
        compileAndBuildSuites(sourceRoot, sourceFilePaths, false);
        List<String> groupList = getGroupList();
        if (groupList.size() == 0) {
            outStream.println("There are no groups available!");
        } else {
            outStream.println("Following groups are available : ");
            outStream.println(groupList);
        }
    }

    /**
     * Returns a distinct list of groups in test functions.
     *
     * @return a list of groups
     */
    public List<String> getGroupList() {

        Map<String, TestSuite> testSuites = registry.getTestSuites();
        if (testSuites.isEmpty()) {
            throw new BallerinaException("No test functions found in the provided ballerina files.");
        }
        List<String> groupList = new ArrayList<>();
        testSuites.forEach((packageName, suite) -> {
            suite.getTests().forEach(test -> {
                if (test.getGroups().size() > 0) {
                    groupList.addAll(test.getGroups());
                }
            });
        });
        return groupList.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Compiles the source and populate the registry with suites.
     * @param sourceRoot source root
     * @param sourceFilePaths List of @{@link Path} of ballerina files
     */
    private void compileAndBuildSuites(String sourceRoot, Path[] sourceFilePaths, boolean buildWithTests)  {
        if (buildWithTests) {
            outStream.println();
        }
        outStream.println("Compiling tests");
        outStream.println();
        Arrays.stream(sourceFilePaths).forEach(sourcePackage -> {

            String packageName = Utils.getFullPackageName(sourcePackage.toString());

            registry.getTestSuites().computeIfAbsent(packageName, func -> new TestSuite
                (packageName));
            // compile
            CompileResult compileResult = BCompileUtil.compileWithTests(sourceRoot, sourcePackage.toString(),
                CompilerPhase.CODE_GEN);
            // print errors
            for (Diagnostic diagnostic : compileResult.getDiagnostics()) {
                errStream.println(diagnostic.getKind() + ": " + diagnostic.getPosition() + " " + diagnostic
                    .getMessage());
            }
            if (compileResult.getErrorCount() > 0) {
                throw new BallerinaException("[ERROR] Compilation failed.");
            }
            // set the debugger
            ProgramFile programFile = compileResult.getProgFile();
            Debugger debugger = new Debugger(programFile);
            Utils.initDebugger(programFile, debugger);
            registry.addProgramFile(programFile);

            // process the compiled files
            ServiceLoader<CompilerPlugin> processorServiceLoader = ServiceLoader.load(CompilerPlugin.class);
            processorServiceLoader.forEach(plugin -> {
                if (plugin instanceof TestAnnotationProcessor) {
                    try {
                        ((TestAnnotationProcessor) plugin).packageProcessed(programFile);
                    } catch (Exception e) {
                        errStream.println("[ERROR] Validation failed. Cause: " + e.getMessage());
                        throw new BallerinaException(e);
                    }
                }
            });
        });
        registry.setTestSuitesCompiled(true);
    }

    /**
     * Run all tests.
     */
    private void execute() {
        Map<String, TestSuite> testSuites = registry.getTestSuites();
        if (testSuites.isEmpty()) {
            throw new BallerinaException("No test functions found in the provided ballerina files.");
        }

        AtomicBoolean shouldSkip = new AtomicBoolean();
        LinkedList<String> keys = new LinkedList<>(testSuites.keySet());
        Collections.sort(keys);

        outStream.println();
        outStream.println("Running Tests");
        outStream.println();
        keys.forEach(packageName -> {
            TestSuite suite = testSuites.get(packageName);
            // outStream.println("---------------------------------------------------------------------------");
//            outStream.println();
//            outStream.println("Running Tests");
            if (!packageName.equals(Names.DOT.value)) {
//                outStream.println("\t" + packageName);
//                outStream.println("\t" + String.join("", Collections.nCopies(packageName.length(), "-")));
                outStream.println("    " + packageName);
                outStream.println();
//                outStream.println(String.join("", Collections.nCopies(packageName.length(), "")));
            }
            // outStream.println("---------------------------------------------------------------------------");
            shouldSkip.set(false);
            TestAnnotationProcessor.injectMocks(suite);
            tReport.addPackageReport(packageName);
            if (suite.getInitFunction() != null && Utils.isPackageInitialized(packageName)) {
                suite.getInitFunction().invoke();
            }

            suite.getBeforeSuiteFunctions().forEach(test -> {
                String errorMsg;
                try {
                    test.invoke();
                } catch (Throwable e) {
                    shouldSkip.set(true);
//                    errorMsg = String.format("Failed to execute before test suite function [%s] of test suite " +
//                                             "package [%s]. Cause: %s", test.getName(), packageName, e.getMessage());
                    errorMsg = "\t✗ " + test.getName() + " [before test suite function]" + ":\n\t    "
                            + e.getMessage().replaceAll("\n", "\n\t    ");
                    errStream.println(errorMsg);
                }
            });
            List<String> failedOrSkippedTests = new ArrayList<>();
            suite.getTests().forEach(test -> {
                AtomicBoolean shouldSkipTest = new AtomicBoolean(false);
                if (!shouldSkip.get() && !shouldSkipTest.get()) {
                    // run the beforeEach tests
                    suite.getBeforeEachFunctions().forEach(beforeEachTest -> {
                        String errorMsg;
                        try {
                            beforeEachTest.invoke();
                        } catch (Throwable e) {
                            shouldSkipTest.set(true);
//                            errorMsg = String.format("\tFailed to execute before each test function [%s] for the "
//                                                     + "test [%s] of test suite package [%s]. Cause: %s",
//                                    beforeEachTest.getName(),
//                                    test.getTestFunction().getName(), packageName, e.getMessage());
                            errorMsg = String.format("\t✗ " +  beforeEachTest.getName() +
                                                             " [before each test function for test %s] :\n\t    %s",
                                                     test.getTestFunction().getName(),
                                                     e.getMessage().replaceAll("\n", "\n\t    "));
                            errStream.println(errorMsg);
                        }
                    });
                }
                if (!shouldSkip.get() && !shouldSkipTest.get()) {
                    // run before tests
                    String errorMsg;
                    try {
                        if (test.getBeforeTestFunctionObj() != null) {
                            test.getBeforeTestFunctionObj().invoke();
                        }
                    } catch (Throwable e) {
                        shouldSkipTest.set(true);
//                        errorMsg = String.format("\tFailed to execute before test function" + " [%s] for the test " +
//                                                 "[%s] of test suite package [%s]. Cause: %s",
//                                test.getBeforeTestFunctionObj().getName
//                                        (), test.getTestFunction().getName(), packageName, e.getMessage());

                        errorMsg = String.format("\t✗ " +  test.getBeforeTestFunctionObj().getName() +
                                                         " [before test function for test %s] :\n\t    %s",
                                                 test.getTestFunction().getName(),
                                                 e.getMessage().replaceAll("\n", "\n\t    "));
                        errStream.println(errorMsg);
                    }
                }
                // run the test
                TesterinaResult functionResult = null;
                try {
                    if (isTestDependsOnFailedFunctions(test.getDependsOnTestFunctions(), failedOrSkippedTests)) {
                      shouldSkipTest.set(true);
                    }

                    // Check whether the this test depends on any failed or skipped functions
                    if (!shouldSkip.get() && !shouldSkipTest.get()) {
                        BValue[] valueSets = null;
                        if (test.getDataProviderFunction() != null) {
                            valueSets = test.getDataProviderFunction().invoke();
                        }
                        if (valueSets == null) {
                            test.getTestFunction().invoke();
                            // report the test result
                            functionResult = new TesterinaResult(test.getTestFunction().getName(), true, shouldSkip
                                .get(), null);
                            tReport.addFunctionResult(packageName, functionResult);
                        } else {
                            List<BValue[]> argList = extractArguments(valueSets);
                            argList.forEach(arg -> {
                                test.getTestFunction().invoke(arg);
                                TesterinaResult result = new TesterinaResult(test.getTestFunction().getName(), true,
                                        shouldSkip.get(), null);
                                tReport.addFunctionResult(packageName, result);
                            });
                        }
                    } else {
                        // If the test function is skipped lets add it to the failed test list
                        failedOrSkippedTests.add(test.getTestFunction().getName());
                        // report the test result
                        functionResult = new TesterinaResult(test.getTestFunction().getName(), false, true, null);
                        tReport.addFunctionResult(packageName, functionResult);
                    }
                } catch (Throwable e) {
                    // If the test function is skipped lets add it to the failed test list
                    failedOrSkippedTests.add(test.getTestFunction().getName());
//                    String errorMsg = String.format("Failed to execute the test function [%s] of test suite package "
//                            + "[%s]. Cause: %s", test.getTestFunction().getName(), packageName, e.getMessage());
////                    errStream.println(errorMsg);
                    // report the test result
                    functionResult = new TesterinaResult(test.getTestFunction().getName(), false, shouldSkip.get(),
                            e.getMessage());
                    tReport.addFunctionResult(packageName, functionResult);
                }

                // run the after tests
                String error;
                try {
                    if (test.getAfterTestFunctionObj() != null) {
                        test.getAfterTestFunctionObj().invoke();
                    }
                } catch (Throwable e) {
//                    error = String.format("\tFailed to execute after test function" + " [%s] for the test [%s] of test " +
//                                          "suite package [%s]. Cause: %s", test.getAfterTestFunctionObj().getName(),
//                            test.getTestFunction().getName(), packageName, e.getMessage());


                    error = String.format("\t✗ " +  test.getAfterTestFunctionObj().getName() +
                                                     " [after test function for test %s] :\n\t    %s",
                                             test.getTestFunction().getName(),
                                          e.getMessage().replaceAll("\n", "\n\t    "));
                    errStream.println(error);
                }

                // run the afterEach tests
                suite.getAfterEachFunctions().forEach(afterEachTest -> {
                    String errorMsg2;
                    try {
                        afterEachTest.invoke();
                    } catch (Throwable e) {
//                        errorMsg2 = String.format("\tFailed to execute after each test function" + " [%s] for the test " +
//                                                  "[%s] of test suite package [%s]. Cause: %s", afterEachTest.getName(),
//                                test.getTestFunction().getName(), packageName, e.getMessage());

                        errorMsg2 = String.format("\t✗ " +  afterEachTest.getName() +
                                                      " [after each test function for test %s] :\n\t    %s",
                                              test.getTestFunction().getName(),
                                                  e.getMessage().replaceAll("\n", "\n\t    "));
                        errStream.println(errorMsg2);
                    }
                });
            });
            TestAnnotationProcessor.resetMocks(suite);

            // Run After suite functions
            suite.getAfterSuiteFunctions().forEach(func -> {
                String errorMsg;
                try {
                    func.invoke();
                } catch (Throwable e) {
//                    errorMsg = String.format("\tFailed to execute after test suite function [%s] of test suite " +
//                                             "package [%s]. Cause: %s", func.getName(), packageName, e.getMessage());

                    errorMsg = String.format("\t✗ " +  func.getName() +
                                                      " [after test suite function] :\n\t    %s",
                                             e.getMessage().replaceAll("\n", "\n\t    "));
                    errStream.println(errorMsg);
                }
            });
            // print package test results
            tReport.printTestSuiteSummary(packageName);
        });
    }

    private boolean isTestDependsOnFailedFunctions(List<String> failedOrSkippedTests, List<String> dependentTests) {
        return failedOrSkippedTests.stream().parallel().anyMatch(dependentTests::contains);
    }

    /**
     * Extract function arguments from the values sets.
     * @param valueSets user provided value sets
     * @return a list of function arguments
     */
    private List<BValue[]> extractArguments(BValue[] valueSets) {
        List<BValue[]> argsList = new ArrayList<>();

        for (BValue value : valueSets) {
            if (value instanceof BRefValueArray) {
                BRefValueArray array = (BRefValueArray) value;
                for (BIterator it = array.newIterator(); it.hasNext(); ) {
                    BValue[] vals = it.getNext(0);
                    if (vals[1] instanceof BNewArray) {
                        BNewArray bNewArray = (BNewArray) vals[1];
                        BValue[] args = new BValue[(int) bNewArray.size()];
                        for (int j = 0; j < bNewArray.size(); j++) {
                            args[j] = bNewArray.getBValue(j);
                        }
                        argsList.add(args);
                    } else {
                        // cannot happen due to validations done at annotation processor
                    }
                }
            } else if (value instanceof BJSON) {
                BJSON jsonArrayOfArrays = (BJSON) value;
                for (BIterator it = jsonArrayOfArrays.newIterator(); it.hasNext(); ) {
                    BValue[] vals = it.getNext(0);
                    if (vals[1] instanceof BJSON) {
                        List<BValue> args = new ArrayList<>();
                        BJSON jsonArray = (BJSON) vals[1];
                        for (BIterator it2 = jsonArray.newIterator(); it2.hasNext(); ) {
                            BValue[] vals2 = it2.getNext(0);
                            args.add(vals2[1]);
                        }
                        argsList.add(args.toArray(new BValue[0]));
                    } else {
                        // cannot happen due to validations done at annotation processor
                    }
                }
            } else {
                argsList.add(new BValue[]{value});
            }
        }
        return argsList;
    }

    /**
     * Return the Test report of program runner.
     *
     * @return {@link TesterinaReport} object
     */
    public TesterinaReport getTesterinaReport() {
        return tReport;
    }

}

