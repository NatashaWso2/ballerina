/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.nativeimpl.compression;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Native function ballerina.compression:unzipBytes.
 *
 * @since 0.8.0
 */
@BallerinaFunction(
        packageName = "ballerina.compression",
        functionName = "unzipBytes",
        args = {@Argument(name = "content", type = TypeKind.BLOB),
                @Argument(name = "destDir", type = TypeKind.STRING)},
        isPublic = true
)
public class UnzipBytes extends AbstractNativeFunction {

    private static final Logger log = LoggerFactory.getLogger(UnzipBytes.class);

    /**
     * File content as byte array defined in ballerina.compression
     */
    private static final int SRC_AS_BYTEARRAY_FIELD_INDEX = 0;

    /**
     * File path of the destination directory defined in ballerina.compression
     */
    private static final int DEST_PATH_FIELD_INDEX = 0;

    /**
     * Decompress/unzip byte arrays/blob
     *
     * @param fileContentAsByteArray file content as a byte arry
     * @param outputFolder           destination folder
     */
    protected static void decompress(byte[] fileContentAsByteArray, String outputFolder) {
        final int BUFFER = 1024;
        int count;
        byte data[] = new byte[BUFFER];
        BufferedOutputStream bufferedOutputStream;
        ByteArrayInputStream bis = new ByteArrayInputStream(fileContentAsByteArray);
        ZipInputStream zipInputStream =
                new ZipInputStream(bis);
        ZipEntry zipEntry;
        //create output directory is not exists
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        try {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                log.debug("Extracting: " + zipEntry);
                File file = new File(outputFolder + zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }
                FileOutputStream fileOutputStream = new FileOutputStream(file, false);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream, BUFFER);

                while ((count = zipInputStream.read(data, 0, BUFFER)) != -1)
                    bufferedOutputStream.write(data, 0, count);

                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            }
            zipInputStream.close();
        } catch (IOException e) {
            log.debug("I/O Exception when processing files ", e);
            log.error("I/O Exception when processing files " + e.getMessage());
        }
    }

    @Override
    public BValue[] execute(Context context) {
        byte[] content = getBlobArgument(context, SRC_AS_BYTEARRAY_FIELD_INDEX);
        String destDir = getStringArgument(context, DEST_PATH_FIELD_INDEX);
        decompress(content, destDir);
        return VOID_RETURN;
    }
}
