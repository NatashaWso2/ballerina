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
import org.ballerinalang.packerina.toml.model.Manifest;
import org.ballerinalang.packerina.toml.model.Settings;
import org.ballerinalang.packerina.toml.parser.ManifestProcessor;
import org.ballerinalang.packerina.toml.parser.SettingsProcessor;
import org.wso2.ballerinalang.util.ExecutorUtils;
import org.wso2.ballerinalang.util.HomeRepoUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * This class provides util methods when pushing Ballerina packages to central and home repository.
 *
 * @since 0.95.2
 */
public class PushUtils {
    private static PrintStream outStream = System.err;

    /**
     * Push/Uploads packages to the central repository.
     *
     * @param packageName   path of the package folder to be pushed
     * @param installToRepo if it should be pushed to central or home
     */
    public static void pushPackages(String packageName, String installToRepo) {
        String accessToken = getAccessTokenOfCLI() != null ? removeQuotationsFromValue(getAccessTokenOfCLI()) : null;
        if (accessToken == null) {
            throw new BLangCompilerException("You have not specified an access-token for the central in your" +
                    " Settings.toml\n. Please login to central if you are already registered using" +
                    " 'central.ballerina.io/login' to get a valid access-token. \nIf you are new to the site please" +
                    "register using 'central.ballerina.io/register'");
        }
        Manifest manifest = readManifestConfigurations();
        if (manifest.getName() == null && manifest.getVersion() == null) {
            throw new BLangCompilerException("An org-name and package version is required when pushing. " +
                    "This is not specified in Ballerina.toml inside the project");
        }
        String orgName = removeQuotationsFromValue(manifest.getName());
        String version = removeQuotationsFromValue(manifest.getVersion());

        Path prjDirPath = Paths.get(".").toAbsolutePath().normalize().resolve(".ballerina");
        Path pkgPath = Paths.get(prjDirPath.toString(), "repo", orgName, packageName, version);

        if (Files.notExists(pkgPath)) {
            throw new BLangCompilerException("Package " + packageName + " of version " + version + " doesn't " +
                    "exist");
        }
        if (installToRepo == null) {
            // Push package to central
            String archiveExt = pkgPath.resolve(packageName + ".zip").toString();
            String resourcePath = "https://staging.central.ballerina.io:9090/" + Paths.get(orgName,
                    packageName, version);
            URI balxPath = URI.create(String.valueOf(PushUtils.class.getClassLoader().getResource
                    ("ballerina.push.balx")));
            ExecutorUtils.execute(balxPath, accessToken, resourcePath, archiveExt);
        } else {
            if (!installToRepo.equals("home")) {
                throw new BLangCompilerException("No repository provided to push the package");
            }
            Path balHomeDir = HomeRepoUtils.createAndGetHomeReposPath();
            Path targetDirectoryPath = balHomeDir.resolve(orgName).resolve(packageName).resolve(version);
            if (Files.exists(targetDirectoryPath)) {
                outStream.println("Ballerina package already in the user repository");
            } else {
                try {
                    Files.createDirectories(targetDirectoryPath);
                    copyDir(pkgPath.toString(), targetDirectoryPath.toString());
                    outStream.println("Ballerina package pushed to the user repository successfully");
                } catch (IOException e) {
                    throw new BLangCompilerException("Error when occured when creating directories in " +
                            "./ballerina/artifacts/ to install the package locally");
                }
            }
        }

    }

    /**
     * Read the manifest.
     *
     * @return manifest configuration object
     */
    private static Manifest readManifestConfigurations() {
        String tomlFilePath = Paths.get(".").toAbsolutePath().normalize().resolve("Ballerina.toml").toString();
        try {
            return ManifestProcessor.parseTomlContentFromFile(tomlFilePath);
        } catch (IOException e) {
            return new Manifest();
        }
    }

    /**
     * Read Settings.toml to populate the configurations.
     *
     * @return settings object
     */
    private static Settings readSettings() {
        String tomlFilePath = UserRepositoryUtils.initializeUserRepository().resolve("Settings.toml").toString();
        try {
            return SettingsProcessor.parseTomlContentFromFile(tomlFilePath);
        } catch (IOException e) {
            return new Settings();
        }
    }

    /**
     * Read the access token generated for the CLI.
     *
     * @return access token for generated for the CLI
     */
    private static String getAccessTokenOfCLI() {
        Settings settings = readSettings();
        if (settings.getCentral() != null) {
            return settings.getCentral().getAccessToken();
        }
        return null;
    }

    /**
     * Remove enclosing quotation from the string value.
     *
     * @param value string value with enclosing quotations
     * @return string value after removing the enclosing quotations
     */
    private static String removeQuotationsFromValue(String value) {
        return value.replace("\"", "");
    }

    /**
     * Copy folders/files from source directory to the destination directory.
     *
     * @param src  source directory path
     * @param dest destination directory path
     */
    private static void copyDir(String src, String dest) throws IOException {
        Files.walk(Paths.get(src)).forEach(a -> {
            Path b = Paths.get(dest, a.toString().substring(src.length()));
            if (!a.toString().equals(src)) {
                try {
                    Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BLangCompilerException("Error occurred when pushing package to the user repository");
                }
            }
        });
    }
}
