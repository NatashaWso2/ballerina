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
package org.ballerinalang.launcher.toml.model.fields;

import org.ballerinalang.launcher.toml.model.Proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Proxy object fields
 */
public enum ProxyField {
    HOST(Proxy::setHost),
    PORT(Proxy::setPort),
    USERNAME(Proxy::setUserName),
    PASSWORD(Proxy::setPassword);

    public static final Map<String, ProxyField> lookup = new HashMap<>();

    static {
        for (ProxyField proxyField : ProxyField.values()) {
            lookup.put(proxyField.name().toLowerCase(), proxyField);
        }
    }

    private final BiConsumer<Proxy, String> stringSetter;

    /**
     * Constructor which sets the string value
     *
     * @param stringSetter string value to be set
     */
    ProxyField(BiConsumer<Proxy, String> stringSetter) {
        this.stringSetter = stringSetter;
    }

    /**
     * Set values to the proxy object
     *
     * @param proxy proxy object
     * @param value value to be set
     */
    public void setValueTo(Proxy proxy, String value) {
        stringSetter.accept(proxy, value);
    }

}
