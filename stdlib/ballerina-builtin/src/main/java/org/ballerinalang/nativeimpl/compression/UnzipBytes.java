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
        try {
            File outdir = new File(outputFolder);
            ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(fileContentAsByteArray));
            ZipEntry entry;
            String name, dir;
            while ((entry = zin.getNextEntry()) != null) {
                name = entry.getName();
                if (entry.isDirectory()) {
                    mkdirs(outdir, name);
                    continue;
                }
        /* this part is necessary because file entry can come before
         * directory entry where is file located
         */
                dir = dirpart(name);
                if (dir != null)
                    mkdirs(outdir, dir);

                extractFile(zin, outdir, name);
            }
            zin.close();
        } catch (IOException e) {
            log.debug("I/O Exception when processing files ", e);
            log.error("I/O Exception when processing files " + e.getMessage());
        }
    }

    /**
     * Extract files from the zipInputStream
     *
     * @param in
     * @param outdir
     * @param name
     * @throws IOException
     */
    private static void extractFile(ZipInputStream in, File outdir, String name) throws IOException {
        byte[] buffer = new byte[4096];
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(outdir, name)));
        int count = -1;
        while ((count = in.read(buffer)) != -1)
            out.write(buffer, 0, count);
        out.close();
    }

    /**
     * Create the directory name
     *
     * @param name
     * @return
     */
    private static String dirpart(String name) {
        int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }

    /**
     * Make directories if they doesn't exists
     *
     * @param outdir
     * @param path
     */
    private static void mkdirs(File outdir, String path) {
        File d = new File(outdir, path);
        if (!d.exists())
            d.mkdirs();
    }

    @Override
    public BValue[] execute(Context context) {
        byte[] content = getBlobArgument(context, SRC_AS_BYTEARRAY_FIELD_INDEX);
        String destDir = getStringArgument(context, DEST_PATH_FIELD_INDEX);
        decompress(content, destDir);
        return VOID_RETURN;
    }
}
