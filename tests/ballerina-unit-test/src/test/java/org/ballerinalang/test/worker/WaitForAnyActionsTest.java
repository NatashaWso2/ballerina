/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.worker;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.exceptions.BLangRuntimeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * Wait for any action related tests.
 *
 * @since 0.985.0
 */
public class WaitForAnyActionsTest {

    private CompileResult result;

    @BeforeClass
    public void setup() {
        this.result = BCompileUtil.compile("test-src/workers/wait-for-any-actions.bal");
        Assert.assertEquals(result.getErrorCount(), 0, "Wait for any actions test error count");
    }

    @Test
    public void waitTest1() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest1", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        // TODO: 11/21/18 Since we are dealing with threads we can't ensure which will be returned first, so atm we
        // check if either values are returned. Need to fix this in a proper way
        Assert.assertTrue(Arrays.asList("7", "22").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest2() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest2", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("82", "22", "2").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest3() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest3", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("hello foo", "hello bar").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest4() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest4", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("30", "hello bar", "true").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest5() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest5", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("{\"line1\":\"No. 20\", \"line2\":\"Palm Grove\", \"city\":\"Colombo 03\"}",
                                        "{\"fname\": \"foo\", \"lname\": \"bar\"}").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest6() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest6", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("176", "150", "hello foo").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest7() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest7", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("4", "22", "true", "hello foo").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest8() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest8", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("hello xyz", "28").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest9() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest9", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("hello xyz", "99").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest10() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest10", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertTrue(Arrays.asList("77", "hello foo", "hello bar", "hello xyz").contains(vals[0].stringValue()));
    }

    @Test
    public void waitTest11() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest11", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertEquals(vals[0].stringValue(), "hello foo");
    }

    @Test(expectedExceptions = {BLangRuntimeException.class},
            expectedExceptionsMessageRegExp = "error: err from panic \\{\\}.*")
    public void waitTest12() {
        BRunUtil.invoke(result, "waitTest12", new BValue[0]);
    }

    @Test
    public void waitTest13() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest13", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertEquals("0", vals[0].stringValue());
    }

    @Test
    public void waitTest14() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest14", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertEquals("err returned {}", vals[0].stringValue());
    }

    @Test
    public void waitTest15() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest15", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertEquals("hello moo", vals[0].stringValue());
    }

    @Test(expectedExceptions = {BLangRuntimeException.class},
            expectedExceptionsMessageRegExp = "error: err from panic \\{\\}.*")
    public void waitTest16() {
        BRunUtil.invoke(result, "waitTest16", new BValue[0]);
    }

    @Test
    public void waitTest17() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest17", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertEquals("hello foo", vals[0].stringValue());
    }

    @Test
    public void waitTest18() {
        BValue[] vals = BRunUtil.invoke(result, "waitTest18", new BValue[0]);
        Assert.assertEquals(vals.length, 1);
        Assert.assertEquals(null, vals[0]);
    }
}
