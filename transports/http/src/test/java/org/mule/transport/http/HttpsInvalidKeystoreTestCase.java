/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;
import org.mule.transport.ConnectException;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpsInvalidKeystoreTestCase extends FunctionalTestCase implements ExceptionCallback
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    private Throwable exceptionFromSystemExceptionHandler;

    public HttpsInvalidKeystoreTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "https-invalid-keystore-config.xml";
    }

    @Test
    public void startingSslMessageReceiverWithoutKeystoreShouldThrowConnectException() throws Exception
    {
        TestExceptionStrategy exceptionListener = new TestExceptionStrategy();
        exceptionListener.setExceptionCallback(this);

        muleContext.setExceptionListener(exceptionListener);
        muleContext.start();

        assertNotNull(exceptionFromSystemExceptionHandler);
        assertTrue(exceptionFromSystemExceptionHandler instanceof ConnectException);
        assertTrue(exceptionFromSystemExceptionHandler.getMessage().contains("tls-key-store"));
    }

    public void onException(Throwable t)
    {
        exceptionFromSystemExceptionHandler = t;
    }
}


