/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.tck.testmodels.mule.TestExceptionStrategy.ExceptionCallback;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * This tests single resource transactions through a single service definition (even
 * though the mule config file will have many service definitions). The idea is to
 * test all possible combinations of single resource transaction types.
 *
 * @author dzapata
 */
public abstract class AbstractJmsSingleTransactionSingleServiceTestCase extends AbstractJmsFunctionalTestCase
{
    // queue names
    public static final String JMS_QUEUE_INPUT_CONF_A = "in1";
    public static final String JMS_QUEUE_OUTPUT_CONF_A = "out1";
    public static final String JMS_QUEUE_INPUT_CONF_B = "in2";
    public static final String JMS_QUEUE_OUTPUT_CONF_B = "out2";
    public static final String JMS_QUEUE_INPUT_CONF_C = "in3";
    public static final String JMS_QUEUE_OUTPUT_CONF_C = "out3";
    public static final String JMS_QUEUE_INPUT_CONF_D = "in4";
    public static final String JMS_QUEUE_OUTPUT_CONF_D = "out4";
    public static final String JMS_QUEUE_INPUT_CONF_E = "in5";
    public static final String JMS_QUEUE_OUTPUT_CONF_E = "out5";

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleContext.setExceptionListener(new TestExceptionStrategy());
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties props = super.getStartUpProperties();
        // Inject endpoint names into the config
        props.put(INBOUND_ENDPOINT_KEY + "1", getJmsConfig().getInboundEndpoint() + "1");
        props.put(INBOUND_ENDPOINT_KEY + "2", getJmsConfig().getInboundEndpoint() + "2");
        props.put(INBOUND_ENDPOINT_KEY + "3", getJmsConfig().getInboundEndpoint() + "3");
        props.put(INBOUND_ENDPOINT_KEY + "4", getJmsConfig().getInboundEndpoint() + "4");
        props.put(INBOUND_ENDPOINT_KEY + "5", getJmsConfig().getInboundEndpoint() + "5");

        props.put(OUTBOUND_ENDPOINT_KEY + "1", getJmsConfig().getOutboundEndpoint() + "1");
        props.put(OUTBOUND_ENDPOINT_KEY + "2", getJmsConfig().getOutboundEndpoint() + "2");
        props.put(OUTBOUND_ENDPOINT_KEY + "3", getJmsConfig().getOutboundEndpoint() + "3");
        props.put(OUTBOUND_ENDPOINT_KEY + "4", getJmsConfig().getOutboundEndpoint() + "4");
        props.put(OUTBOUND_ENDPOINT_KEY + "5", getJmsConfig().getOutboundEndpoint() + "5");

        return props;
    }

    @Test
    public void testNone() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);

        runTransactionPass();
    }

    @Test
    public void testAlwaysBegin() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);

        runTransactionPass();
    }

    @Test
    public void testBeginOrJoin() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);

        runTransactionPass();
    }

    @Test
    public void testAlwaysJoin() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_D);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_D);

        runTransactionPass();
    }

    @Test
    public void testJoinIfPossible() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_E);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_E);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_E);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_E);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_E);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_E);

        runTransactionPass();
    }

    /**
     * Call this when you expect the transaction to fail
     * @param serviceName
     * @throws Exception
     */
    protected void runTransactionFail(String serviceName) throws Exception
    {
        final CountDownLatch exceptionLatch = new CountDownLatch(1);

        send(scenarioCommit);

        final ExceptionCallback exceptionCallback = new ExceptionCallback()
        {
            @Override
            public void onException(Throwable t)
            {
                assertTrue(ExceptionUtils.containsType(t,
                        org.mule.runtime.core.transaction.IllegalTransactionStateException.class));
                assertEquals(1, exceptionLatch.getCount()); // make sure this
                                                            // exception doesn't
                                                            // happen more than once
                exceptionLatch.countDown();
            }
        };
        TestExceptionStrategy exceptionStrategy = (TestExceptionStrategy) muleContext.getRegistry()
            .lookupFlowConstruct(serviceName)
            .getExceptionListener();
        exceptionStrategy.setExceptionCallback(exceptionCallback);

        TestExceptionStrategy globalExceptionStrategy = (TestExceptionStrategy) muleContext.getExceptionListener();
        globalExceptionStrategy.setExceptionCallback(exceptionCallback);

        assertTrue(exceptionLatch.await(10, TimeUnit.SECONDS));
        receive(scenarioNotReceive);
    }

    /**
     * Call this when you expect the message to make it to the outbound endpoint
     * @throws Exception
     */
    protected void runTransactionPass() throws Exception
    {
        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }
}
