/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service.foo;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.service.test.api.EchoService;
import org.mule.runtime.service.test.api.FooService;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class FooServiceProvider implements ServiceProvider {
    @Inject
    private EchoService echoService;

    public FooServiceProvider() {
    }

    public List<ServiceDefinition> providedServices() {
        return Collections.singletonList(new ServiceDefinition(FooService.class, new DefaultFooService(this.echoService)));
    }
}
