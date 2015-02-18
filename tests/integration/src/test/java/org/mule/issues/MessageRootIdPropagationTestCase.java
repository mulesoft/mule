/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractMessageTransformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

public class MessageRootIdPropagationTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/issues/message-root-id.xml";
    }

    @Test
    public void testRootIDs() throws Exception
    {
        RootIDGatherer.initialize();
        MuleClient client = muleContext.getClient();

        MuleMessage msg = getTestMuleMessage("Hello");
        msg.setOutboundProperty("where", "client");
        RootIDGatherer.process(msg);
        client.send("vm://vmin", msg);
        Thread.sleep(1000);
        assertEquals(6, RootIDGatherer.getMessageCount());
        assertEquals(1, RootIDGatherer.getIds().size());
    }

    static class RootIDGatherer extends AbstractMessageTransformer
    {
        static int messageCount;
        static Map<String, String>idMap;
        static int counter;


        public static void initialize()
        {
            idMap = new HashMap<String, String>();
            messageCount = 0;
        }

        public static synchronized void process(MuleMessage msg)
        {
            String id = msg.getMessageRootId();
            messageCount++;
            String where = msg.<String>getOutboundProperty("where");
            if (where == null)
            {
                where = "location_" + counter++;
            }
            idMap.put(where, id);
        }

        @Override
        public Object transformMessage(MuleMessage msg, String outputEncoding)
        {
            process(msg);
            return msg.getPayload();
        }

        public static Set<String> getIds()
        {
            return new HashSet<String>(idMap.values());
        }

        public static int getMessageCount()
        {
            return messageCount;
        }

        public static Map<String, String> getIdMap()
        {
            return idMap;
        }
    }
}
