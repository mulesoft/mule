/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.classloader;

import static org.junit.Assert.assertThat;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.listener.Callback;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.test.infrastructure.deployment.FakeMuleServer;

import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConnectorLevelMessageDispatchingTestCase extends AbstractMuleTestCase
{

    public static final String HELLO_WORLD_APP = "hello-world";
    public static final String HELLO_MULE_APP = "hello-mule";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public SystemProperty endpointScheme = new SystemProperty("scheme", "http");

    @Test
    public void verifyClassLoaderIsAppClassLoader() throws Exception
    {
        FakeMuleServer fakeMuleServer = new FakeMuleServer(temporaryFolder.getRoot().getAbsolutePath());
        fakeMuleServer.deployDomainFromClasspathFolder("domain/deployable-domains/http-connector-domain", "domain");
        fakeMuleServer.deployAppFromClasspathFolder("domain/deployable-apps/hello-world-app", HELLO_WORLD_APP);
        fakeMuleServer.deployAppFromClasspathFolder("domain/deployable-apps/hello-mule-app", HELLO_MULE_APP);
        fakeMuleServer.start();
        verifyAppProcessMessageWithAppClassLoader(fakeMuleServer, HELLO_MULE_APP, "http://localhost:%d/service/helloMule");
        verifyAppProcessMessageWithAppClassLoader(fakeMuleServer, HELLO_WORLD_APP, "http://localhost:%d/service/helloWorld");
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
