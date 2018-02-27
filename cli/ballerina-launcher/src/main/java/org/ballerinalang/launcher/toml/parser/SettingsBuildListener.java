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
package org.ballerinalang.launcher.toml.parser;

import org.ballerinalang.launcher.toml.antlr4.TomlBaseListener;
import org.ballerinalang.launcher.toml.antlr4.TomlParser;
import org.ballerinalang.launcher.toml.model.Central;
import org.ballerinalang.launcher.toml.model.Proxy;
import org.ballerinalang.launcher.toml.model.Settings;
import org.ballerinalang.launcher.toml.model.fields.CentralField;
import org.ballerinalang.launcher.toml.model.fields.ProxyField;
import org.ballerinalang.launcher.toml.model.fields.SettingHeaders;
import org.ballerinalang.launcher.toml.util.SingletonStack;

import java.util.List;

/**
 * Custom listener which is extended from the Toml listener with our own custom logic.
 */
public class SettingsBuildListener extends TomlBaseListener {
    private final Settings settings;
    private Proxy proxy = new Proxy();
    private Central central = new Central();
    private String currentHeader = null;
    private SingletonStack currentKey = new SingletonStack();

    /**
     * Cosntructor with the settings object.
     *
     * @param settings object
     */
    SettingsBuildListener(Settings settings) {
        this.settings = settings;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     *
     * @param ctx
     */
    @Override
    public void enterKeyval(TomlParser.KeyvalContext ctx) {
        currentKey.push(ctx.key().getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     *
     * @param ctx
     */
    @Override
    public void enterString(TomlParser.StringContext ctx) {
        setToManifest(ctx.getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     *
     * @param ctx
     */
    @Override
    public void exitKeyval(TomlParser.KeyvalContext ctx) {
        setSettingObj();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>The default implementation does nothing.</p>
     *
     * @param ctx
     */
    @Override
    public void enterStdTable(TomlParser.StdTableContext ctx) {
        addHeader(ctx.key());
    }

    /**
     * Add the dependencies and patches to the manifest object.
     */
    private void setSettingObj() {
        if (SettingHeaders.CENTRAL.stringEquals(currentHeader)) {
            this.settings.setCentral(central);
        } else if (SettingHeaders.PROXY.stringEquals(currentHeader)) {
            this.settings.setProxy(proxy);
        }
    }

    /**
     * Add the key-value pairs specified in the toml file.
     *
     * @param value KeyvalContext object
     */
    private void setToManifest(String value) {
        if (currentKey.present()) {
            if (SettingHeaders.PROXY.stringEquals(currentHeader)) {
                ProxyField proxyField = ProxyField.LOOKUP.get(currentKey.pop());
                if (proxyField != null) {
                    proxyField.setValueTo(proxy, value);
                }
            } else if (SettingHeaders.CENTRAL.stringEquals(currentHeader)) {
                CentralField centralField = CentralField.LOOKUP.get(currentKey.pop());
                if (centralField != null) {
                    centralField.setValueTo(central, value);
                }
            }
        }
    }

    /**
     * Add table headers in the toml file.
     *
     * @param keyContextList list of keys specified in the header
     */
    private void addHeader(List<TomlParser.KeyContext> keyContextList) {
        currentHeader = keyContextList.get(0).getText();
    }
}
