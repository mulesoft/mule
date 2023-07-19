/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.foo;

import org.mule.runtime.service.test.api.EchoService;
import org.mule.runtime.service.test.api.FooService;

import javax.inject.Inject;

public class DefaultFooService implements FooService {
    @Inject
    private EchoService echoService;

    public DefaultFooService(EchoService echoService) {
        this.echoService = echoService;
    }

    public String doFoo(String foo) {
        return this.echoService.echo(foo);
    }

    public String getName() {
        return "DefaultFooService";
    }
}
