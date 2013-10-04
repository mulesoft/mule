/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.TransactionNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ExceptionStrategyExceptionPatternTestCase extends AbstractServiceAndFlowTestCase
{

    public static final String PAYLOAD = "some text";
    public static final int TIMEOUT = 5000;
    private Latch exceptionLatch = new Latch();
    private Latch commitLatch = new Latch();
    private Latch rollbackLatch = new Latch();
    private AtomicReference<Exception> exceptionHolder = new AtomicReference<Exception>();

    public ExceptionStrategyExceptionPatternTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{AbstractServiceAndFlowTestCase.ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/exception-strategy-exception-pattern-service.xml"},
                {AbstractServiceAndFlowTestCase.ConfigVariant.FLOW, "org/mule/test/integration/exceptions/exception-strategy-exception-pattern-flow.xml"}});
    }
    
    @Before
    public void setUp() throws Exception
    {
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
        public void onNotification(ExceptionNotification notification)
            {
                exceptionLatch.release();
            }
        });
        FunctionalTestComponent failingFlow = getFunctionalTestComponent("failingFlow");
        failingFlow.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                throw exceptionHolder.get();
            }
        });
        muleContext.registerListener(new TransactionNotificationListener<TransactionNotification>() {
            @Override
            public void onNotification(TransactionNotification notification)
            {
                if (notification.getAction() == TransactionNotification.TRANSACTION_COMMITTED)
                {
                    commitLatch.release();    
                }
                else if (notification.getAction() == TransactionNotification.TRANSACTION_ROLLEDBACK)
                {
                    rollbackLatch.release();                    
                }
            }
        });
    }
    
    @Test
    public void testThrowExceptionAndCommit() throws Exception
    {

        MuleClient client = muleContext.getClient();
        exceptionHolder.set(new IOException());
        client.dispatch("jms://in", PAYLOAD,null);
        if (!exceptionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("exception should be thrown");
        }
        MuleMessage muleMessage = client.request("jms://out", TIMEOUT);
        assertThat(muleMessage, IsNull.notNullValue());
    }

    @Test
    public void testThrowExceptionAndRollback() throws Exception
    {

        MuleClient client = muleContext.getClient();
        exceptionHolder.set(new IllegalArgumentException());
        client.dispatch("jms://in", PAYLOAD,null);
        if (!exceptionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("exception should be thrown");
        }
        MuleMessage muleMessage = client.request("jms://out", TIMEOUT);
        assertThat(muleMessage, IsNull.nullValue());
    }
}
