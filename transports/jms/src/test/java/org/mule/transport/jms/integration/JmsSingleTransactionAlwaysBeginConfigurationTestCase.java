/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

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

    public JmsSingleTransactionAlwaysBeginConfigurationTestCase(JmsVendorConfiguration config)
    {
        super(config);
        setTransacted(true);
    }

    protected String getConfigResources()
    {
        return "integration/jms-single-tx-ALWAYS_BEGIN.xml";
    }

    MessagePostProcessor commit = new MessagePostProcessor() 
    {
        public void postProcess(Session session, Message message) throws JMSException
        {
            session.commit();
        }
    };
    
    MessagePostProcessor rollback = new MessagePostProcessor() 
    {
        public void postProcess(Session session, Message message) throws JMSException
        {
            session.rollback();
        }
    };
    
    @Test
    public void testConfigurationA() throws Exception
    {
        send(JMS_QUEUE_INPUT_CONF_A, DEFAULT_INPUT_MESSAGE, commit);
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, 
            receive(JMS_QUEUE_OUTPUT_CONF_A, getTimeout(), rollback));
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, 
                            receive(JMS_QUEUE_OUTPUT_CONF_A, getTimeout(), commit));
        assertNull(receive(JMS_QUEUE_OUTPUT_CONF_A, getTimeout(), null));
    }

    @Test
    public void testConfigurationB() throws Exception
    {
        send(JMS_QUEUE_INPUT_CONF_B, DEFAULT_INPUT_MESSAGE, commit);
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, 
            receive(JMS_QUEUE_OUTPUT_CONF_B, getTimeout(), rollback));
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, 
            receive(JMS_QUEUE_OUTPUT_CONF_B, getTimeout(), commit));
        assertNull(receive(JMS_QUEUE_OUTPUT_CONF_B, getTimeout(), null));
    }

    @Test
    public void testConfigurationC() throws Exception
    {
        send(JMS_QUEUE_INPUT_CONF_C, DEFAULT_INPUT_MESSAGE, commit);
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, 
            receive(JMS_QUEUE_OUTPUT_CONF_C, getTimeout(), rollback));
        assertPayloadEquals(DEFAULT_OUTPUT_MESSAGE, 
            receive(JMS_QUEUE_OUTPUT_CONF_C, getTimeout(), commit));
        assertNull(receive(JMS_QUEUE_OUTPUT_CONF_C, getTimeout(), null));
    }
}
