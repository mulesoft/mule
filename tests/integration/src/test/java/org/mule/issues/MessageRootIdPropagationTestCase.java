/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.AbstractMessageTransformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MessageRootIdPropagationTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/issues/message-root-id.xml";
    }

    @Test
    public void testRootIDs() throws Exception
    {
        RootIDGatherer.initialize();
        MuleClient client = new MuleClient(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("Hello", muleContext);
        msg.setOutboundProperty("where", "client");
        RootIDGatherer.process(msg);
        MuleMessage response = client.send("vm://vmin", msg);
        Thread.sleep(1000);
        System.out.println(RootIDGatherer.getIdMap());
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
        public Object transformMessage(MuleMessage msg, String encoding)
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
