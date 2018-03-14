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

package org.ballerinalang.nativeimpl.builtin.stringlib;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Native function ballerina.model.strings:replaceAllWithRegex.
 */
@BallerinaFunction(
        orgName = "ballerina",
        packageName = "builtin",
        functionName = "string.replaceAllWithRegex",
        args = {@Argument(name = "mainString", type = TypeKind.STRING),
                @Argument(name = "reg", type = TypeKind.STRUCT, structType = "Regex",
                        structPackage = "builtin"),
                @Argument(name = "replaceWith", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.STRING), @ReturnType(type = TypeKind.STRUCT)},
        isPublic = true
)
public class ReplaceAllWithRegex extends AbstractRegexFunction {

    @Override
    public BValue[] execute(Context context) {

        String mainString = getStringArgument(context, 0);
        String replaceWith = getStringArgument(context, 1);

        BStruct regexStruct = (BStruct) getRefArgument(context, 0);
        try {
            Pattern pattern = validatePattern(regexStruct);

            Matcher matcher = pattern.matcher(mainString);
            String replacedString = matcher.replaceAll(replaceWith);
            return getBValues(new BString(replacedString));
        } catch (PatternSyntaxException e) {
            return getBValues(new BString(""), BLangVMErrors.createError(context, 0, e.getMessage()));
        }
    }
}
