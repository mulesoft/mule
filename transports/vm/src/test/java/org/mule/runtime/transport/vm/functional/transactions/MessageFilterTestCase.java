/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * Test transaction behavior when "joinExternal" is set to disallow joining external
 * transactions There is one test per legal transactional behavior (e.g.
 * ALWAYS_BEGIN).
 */
public class MessageFilterTestCase extends FunctionalTestCase
{

    protected static final Log logger = LogFactory.getLog(MessageFilterTestCase.class);

    private static String rejectMesage;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/config/message-filter-config-flow.xml";
    }

    /** Check that the configuration specifies considers external transactions */
    @Test
    public void testConfiguration() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://order.validation", "OK", null);
        assertNull(response);
        assertEquals("OK(rejected!-1)", rejectMesage);

        response = client.send("vm://order.validation", "OK-ABC", null);
        assertNull(response);
        assertEquals("OK-ABC(rejected!-2)", rejectMesage);

        response = client.send("vm://order.validation", "OK-DEF", null);
        assertNull(response);
        assertEquals("OK-DEF(rejected!-1)", rejectMesage);
        rejectMesage = null;

        response = client.send("vm://order.validation", "OK-ABC-DEF", null);
        assertEquals("OK-ABC-DEF(success)", getPayloadAsString(response));
        assertNull(rejectMesage);
    }

    public static class Reject1 implements MessageProcessor
    {
        public void setName(String name)
        {
            // ignore name
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                String payload = event.getMessageAsString();
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
            // ignore name
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                String payload = event.getMessageAsString();
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
