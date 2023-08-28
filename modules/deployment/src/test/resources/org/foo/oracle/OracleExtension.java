/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.oracle;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "Oracle")
@Operations({OracleOperation.class})
public class OracleExtension {

    @Parameter
    private String url;
    @Parameter
    private String user;
    @Parameter
    private String password;


    public OracleExtension() {}

    public String getUrl() {
        return this.url;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }
}