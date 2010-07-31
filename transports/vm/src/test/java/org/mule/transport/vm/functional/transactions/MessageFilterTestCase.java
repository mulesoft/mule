/*
 * $Id:AbstractExternalTransactionTestCase.java 8215 2007-09-05 16:56:51Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm.functional.transactions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/** Test transaction behavior when "joinExternal" is set to disallow joining external transactions
 * There is one test per legal transactional behavior (e.g. ALWAYS_BEGIN).
 */
public class MessageFilterTestCase extends FunctionalTestCase
{
    protected static final Log logger = LogFactory.getLog(MessageFilterTestCase.class);

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/filter-config.xml";
    }

    /** Check that the configuration specifies considers external transactions */
    public void testConfiguration() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://order.validation", "OK", null);
        assertNotNull(response);
        assertEquals("OK(rejected!-1)", response.getPayloadAsString());
        response = client.send("vm://order.validation", "OK-ABC", null);
        assertNotNull(response);
        assertEquals("OK-ABC(rejected!-2)", response.getPayloadAsString());
        response = client.send("vm://order.validation", "OK-DEF", null);
        assertNotNull(response);
        assertEquals("OK-DEF(rejected!-1)", response.getPayloadAsString());
        response = client.send("vm://order.validation", "OK-ABC-DEF", null);
        assertNotNull(response);
        assertEquals("OK-ABC-DEF", response.getPayloadAsString());
    }

    public static class Reject1 implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                MuleMessage msg = event.getMessage();
                String payload = msg.getPayloadAsString();
                msg.setPayload(payload + "(rejected!-1)");
                return event;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

     public static class Reject2 implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                MuleMessage msg = event.getMessage();
                String payload = msg.getPayloadAsString();
                msg.setPayload(payload + "(rejected!-2)");
                return event;
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }
}