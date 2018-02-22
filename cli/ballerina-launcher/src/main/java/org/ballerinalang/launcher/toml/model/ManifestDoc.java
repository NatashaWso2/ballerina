/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.launcher.toml.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the configurations of the project created using the Ballerina.toml.
 */
public class ManifestDoc {
    private String name;
    private String version;
    private List<Dependency> dependencies = new ArrayList<>();
    private List<Dependency> patches = new ArrayList<>();

    /**
     * Add a patch to the patches list.
     *
     * @param dependency dependency object
     */
    public void addPatches(Dependency dependency) {
        this.patches.add(dependency);
        patches = removeDuplicates(patches);
    }

    /**
     * Get the dependencies list.
     *
     * @return dependencies list
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Add a dependency to the dependencies list.
     *
     * @param dependency dependency object
     */
    public void addDependancy(Dependency dependency) {
        this.dependencies.add(dependency);
        dependencies = removeDuplicates(dependencies);
    }

    /**
     * Get the package name.
     *
     * @return package name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the package name.
     *
     * @param name package name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the version.
     *
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version
     *
     * @param version version of the package.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Remove duplicates from dependencies and patches list.
     *
     * @param list list of elements
     * @return dependencies or patches list without duplicates
     */
    private List<Dependency> removeDuplicates(List<Dependency> list) {
        return list.stream().distinct().collect(Collectors.toList());
    }
}
