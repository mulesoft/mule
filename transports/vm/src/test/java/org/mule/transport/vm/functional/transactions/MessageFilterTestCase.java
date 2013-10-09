/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

/**
 * Test transaction behavior when "joinExternal" is set to disallow joining external
 * transactions There is one test per legal transactional behavior (e.g.
 * ALWAYS_BEGIN).
 */
public class MessageFilterTestCase extends AbstractServiceAndFlowTestCase
{

    protected static final Log logger = LogFactory.getLog(MessageFilterTestCase.class);

    private static String rejectMesage;

    public MessageFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/config/message-filter-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/config/message-filter-config-flow.xml"}});
    }

    /** Check that the configuration specifies considers external transactions */
    @Test
    public void testConfiguration() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage response = client.send("vm://order.validation", "OK", null);
        assertTrue(response.getPayload() instanceof NullPayload);
        assertEquals("OK(rejected!-1)", rejectMesage);

        response = client.send("vm://order.validation", "OK-ABC", null);
        assertTrue(response.getPayload() instanceof NullPayload);
        assertEquals("OK-ABC(rejected!-2)", rejectMesage);

        response = client.send("vm://order.validation", "OK-DEF", null);
        assertTrue(response.getPayload() instanceof NullPayload);
        assertEquals("OK-DEF(rejected!-1)", rejectMesage);
        rejectMesage = null;

        response = client.send("vm://order.validation", "OK-ABC-DEF", null);
        assertEquals("OK-ABC-DEF(success)", response.getPayloadAsString());
        assertNull(rejectMesage);
    }

    public static class Reject1 implements MessageProcessor
    {
        public void setName(String name)
        {
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                MuleMessage msg = event.getMessage();
                String payload = msg.getPayloadAsString();
                rejectMesage = payload + "(rejected!-1)";
                return null;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

    public static class Reject2 implements MessageProcessor
    {
        public void setName(String name)
        {
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                MuleMessage msg = event.getMessage();
                String payload = msg.getPayloadAsString();
                rejectMesage = payload + "(rejected!-2)";
                return null;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }
}
