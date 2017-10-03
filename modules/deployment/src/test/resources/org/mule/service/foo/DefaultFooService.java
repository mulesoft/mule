/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
