/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import java.util.Properties;

import org.junit.Test;

public class JmsSingleTransactionAlwaysBeginConfigurationTestCase extends AbstractJmsFunctionalTestCase
{
    public static final String JMS_QUEUE_INPUT_CONF_A = "in1";
    public static final String JMS_QUEUE_OUTPUT_CONF_A = "out1";
    public static final String JMS_QUEUE_INPUT_CONF_B = "in2";
    public static final String JMS_QUEUE_OUTPUT_CONF_B = "out2";
    public static final String JMS_QUEUE_INPUT_CONF_C = "in3";
    public static final String JMS_QUEUE_OUTPUT_CONF_C = "out3";

    @Override
    protected Properties getStartUpProperties()
    {
        Properties props = super.getStartUpProperties();
        // Inject endpoint names into the config
        props.put(INBOUND_ENDPOINT_KEY + "1", getJmsConfig().getInboundEndpoint() + "1");
        props.put(INBOUND_ENDPOINT_KEY + "2", getJmsConfig().getInboundEndpoint() + "2");
        props.put(INBOUND_ENDPOINT_KEY + "3", getJmsConfig().getInboundEndpoint() + "3");
        props.put(OUTBOUND_ENDPOINT_KEY + "1", getJmsConfig().getOutboundEndpoint() + "1");
        props.put(OUTBOUND_ENDPOINT_KEY + "2", getJmsConfig().getOutboundEndpoint() + "2");
        props.put(OUTBOUND_ENDPOINT_KEY + "3", getJmsConfig().getOutboundEndpoint() + "3");
        return props;
    }

    protected String getConfigResources()
    {
        return "integration/jms-single-tx-ALWAYS_BEGIN.xml";
    }

    @Test
    public void testConfigurationA() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_A);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_A);

        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }

    @Test
    public void testConfigurationB() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_B);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_B);

        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }

    @Test
    public void testConfigurationC() throws Exception
    {
        scenarioCommit.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioRollback.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioNotReceive.setInputDestinationName(JMS_QUEUE_INPUT_CONF_C);
        scenarioCommit.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioRollback.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioNotReceive.setOutputDestinationName(JMS_QUEUE_OUTPUT_CONF_C);

        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }
}
