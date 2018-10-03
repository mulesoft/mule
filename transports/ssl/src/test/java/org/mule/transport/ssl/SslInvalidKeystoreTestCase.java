/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ConnectException;

import org.junit.Rule;
import org.junit.Test;

public class SslInvalidKeystoreTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    private Throwable exceptionFromSystemExceptionHandler;

    public SslInvalidKeystoreTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "ssl-missing-keystore-config.xml";
    }

    @Test
    public void startingSslMessageReceiverWithoutKeystoreShouldThrowConnectException() throws Exception
    {
        try
        {
            muleContext.start();
        }
        catch (Exception e)
        {
            // Lastly catched by DefaultMuleContext, and raised as LifeCycleException
            assertThat(e, instanceOf(LifecycleException.class));

            // Since endpoint connection is done by a retry policy, the expected exception
            // comes inside a retry policy exhaustion exception
            Throwable retryPolicyException = e.getCause();
            assertThat(retryPolicyException, instanceOf(RetryPolicyExhaustedException.class));

            Throwable actualConnectException = retryPolicyException.getCause();
            assertThat(actualConnectException, instanceOf(ConnectException.class));

            assertThat(actualConnectException.getMessage(), containsString("tls-key-store"));

            exceptionFromSystemExceptionHandler = actualConnectException;
        }
        finally
        {
            // Fail if no exception was thrown
            assertThat(exceptionFromSystemExceptionHandler, notNullValue());
        }
    }
}
