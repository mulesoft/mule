/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.classloader;

import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.functional.listener.Callback;
import org.mule.functional.listener.FlowExecutionListener;
import org.mule.rule.UseMuleLog4jContextFactory;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;
import org.mule.test.infrastructure.deployment.FakeMuleServer;

import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class ConnectorLevelMessageDispatchingTestCase extends AbstractFakeMuleServerTestCase
{

    public static final String HELLO_WORLD_APP = "hello-world";
    public static final String HELLO_MULE_APP = "hello-mule";
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public SystemProperty endpointScheme = new SystemProperty("scheme", "http");
    @Rule
    public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

    @Test
    public void verifyClassLoaderIsAppClassLoader() throws Exception
    {
        muleServer.deployDomainFromClasspathFolder("domain/deployable-domains/http-domain-listener", "domain");
        muleServer.deployAppFromClasspathFolder("domain/deployable-apps/hello-world-app", HELLO_WORLD_APP);
        muleServer.deployAppFromClasspathFolder("domain/deployable-apps/hello-mule-app", HELLO_MULE_APP);
        muleServer.start();
        verifyAppProcessMessageWithAppClassLoader(muleServer, HELLO_MULE_APP, "http://localhost:%d/service/helloMule");
        verifyAppProcessMessageWithAppClassLoader(muleServer, HELLO_WORLD_APP, "http://localhost:%d/service/helloWorld");
    }

    private void verifyAppProcessMessageWithAppClassLoader(FakeMuleServer fakeMuleServer, String appName, String requestUrl) throws MuleException
    {
        MuleContext applicationContext = fakeMuleServer.findApplication(appName).getMuleContext();
        final AtomicReference<ClassLoader> executionClassLoader = new AtomicReference<ClassLoader>();
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener(applicationContext);
        flowExecutionListener.addListener(new Callback<MuleEvent>()
        {
            @Override
            public void execute(MuleEvent source)
            {
                executionClassLoader.set(Thread.currentThread().getContextClassLoader());
            }
        });
        applicationContext.getClient().send(String.format(requestUrl, dynamicPort.getNumber()), "test-data", null);
        flowExecutionListener.waitUntilFlowIsComplete();
        assertThat(executionClassLoader.get(), Is.is(applicationContext.getExecutionClassLoader()));
    }


}
