/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.Serializable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreSourceRetryPolicyTestCase extends ExtensionFunctionalTestCase
{

    public static final int TIMEOUT_MILLIS = 1000;
    public static final int POLL_DELAY_MILLIS = 50;
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "petstore-source-retry-policy.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {PetStoreConnectorWithSource.class};
    }

    @After
    public void tearDown()
    {
        PetStoreConnectorWithSource.timesStarted = 0;
    }

    //TODO - MULE-9399 this test case should expect a MuleRuntimeException
    @Test
    public void retryPolicySourceFailOnStart() throws Exception
    {
        exception.expect(LifecycleException.class);
        exception.expectCause(is(instanceOf(MuleRuntimeException.class)));
        try
        {
            startFlow("source-fail-on-start");
        }
        catch (Exception e)
        {
            new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitLambdaProbe(() -> PetStoreConnectorWithSource.timesStarted == 2));
            throw e;
        }
    }

    @Test
    public void retryPolicySourceFailOnException() throws Exception
    {
        startFlow("source-fail-on-exception");
        new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitLambdaProbe(() -> PetStoreConnectorWithSource.timesStarted == 3));
    }

    @Extension(name = "petstore", description = "PetStore Test connector")
    @Xml(namespaceLocation = "http://www.mulesoft.org/schema/mule/petstore", namespace = "petstore")
    @Sources(PetStoreSource.class)
    public static class PetStoreConnectorWithSource extends PetStoreConnector
    {

        public static int timesStarted;
    }

    @Alias("source")
    public static class PetStoreSource extends Source<String, Serializable>
    {

        @UseConfig
        PetStoreConnectorWithSource config;

        @Parameter
        @Optional(defaultValue = "false")
        boolean failOnStart;

        @Parameter
        @Optional(defaultValue = "false")
        boolean failOnException;

        public static boolean failedDueOnException = false;

        @Override
        public void start()
        {
            PetStoreConnectorWithSource.timesStarted++;

            if (failOnStart || failedDueOnException)
            {
                throw new RuntimeException(new ConnectionException("ERROR"));
            }

            if (failOnException)
            {
                failedDueOnException = true;
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> sourceContext.getExceptionCallback().onException(new ConnectionException("ERROR")));
            }
        }

        @Override
        public void stop()
        {

        }
    }

    private void startFlow(String flowName) throws Exception
    {
        ((Flow) getFlowConstruct(flowName)).start();
    }
}
