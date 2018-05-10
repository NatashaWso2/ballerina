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
package org.ballerinalang.packerina;

import org.ballerinalang.compiler.BLangCompilerException;
import org.ballerinalang.spi.EmbeddedExecutor;
import org.ballerinalang.toml.model.Proxy;
import org.ballerinalang.util.EmbeddedExecutorProvider;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class provides util methods when searching for Ballerina packages in the central.
 *
 * @since 0.95.2
 */
public class SearchUtils {

    /**
     * Search for packages in central.
     *
     * @param argument arguments passed
     * @param repositoryPath remote repository URL provided
     */
    public static void searchInCentral(String argument, String repositoryPath) {
        String query = "?q=" + argument;
        String resourcePath = RepoUtils.getRemoteRepoURL();
        if (repositoryPath != null) {
            if (!isValid(repositoryPath)) {
                throw new BLangCompilerException("Remote repository url provided to search for packages is " +
                                                         "invalid");
            }
            resourcePath = repositoryPath;
        }
        Proxy proxy = RepoUtils.readSettings().getProxy();
        EmbeddedExecutor executor = EmbeddedExecutorProvider.getInstance().getExecutor();
        executor.execute("packaging_search/packaging_search.balx", true, resourcePath, query,
                         proxy.getHost(), proxy.getPort(), proxy.getUserName(), proxy.getPassword());
    }

    /**
     * Validates if the url passed is valid.
     *
     * @param url url passed as a string
     * @return true if the url is valid else false
     */
    private static boolean isValid(String url) {
        HttpURLConnection connection = null;
        try {
            URL siteURL = new URL(url);
            connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int code = connection.getResponseCode();
            return code == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
