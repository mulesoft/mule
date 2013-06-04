/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectStore;
import org.mule.api.transport.MessageReceiver;
import org.mule.construct.Flow;
import org.mule.context.notification.CustomMetadataNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.polling.MessageProcessorPollingConnector;
import org.mule.transport.polling.watermark.WatermarkAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.event.Initializable;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class PollingTestCase extends FunctionalTestCase
{

    private static List<String> registeredValues = new ArrayList<String>();
    private static List<CustomMetadataNotification> registeredNotification = new ArrayList<CustomMetadataNotification>();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/polling-config.xml";
    }

    @Before
    public void restoreStatic()
    {
        registeredValues.clear();
        registeredNotification.clear();
    }

    /**
     * Check that a poll without watermark works
     */
    @Test
    public void testPolling() throws Exception
    {
        executePollOf("pollWithoutWatermark");

        Thread.sleep(2000);
        assertNotSame(-1, registeredValues.indexOf("foo"));
    }


    /**
     * Watermark: No object store, no update expression, no object store key present in the object store.
     * Poll with watermark but no Object Stored Defined, and unexistent object store key. It executes the default value
     * and register that values into the flow var with the name of the key. Store the same value in the object store as
     * it has no update expression.
     */
    @Test
    public void pollWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("nameNotDefinedWatermarkObjectStoreFlow");
        Thread.sleep(1000);
        ObjectStore os = getDafultObjectStore();
        assertTrue(os.contains("test"));
        assertEquals("noKey", os.retrieve("test"));
    }

    /**
     * Pre: No object store, no update expression, no object store key present in the object store. The user
     * changes the watermark value in the flow.
     * Pos: The key is stored in the object store with the value that the user set
     */
    @Test
    public void pollChangeKeyValueWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("changeWatermarkWihtNotDefinedWatermarkObjectStoreFlow");
        Thread.sleep(1000);
        ObjectStore os = getDafultObjectStore();
        assertTrue(os.contains("test"));
        assertEquals("keyPresent", os.retrieve("test"));
    }

    /**
     * Pre: No object store, no update expression. The user changes the watermark value in the flow.
     * Pos: The key is stored in the object store with the value that the user set  in the flow, the flow variable of the
     * watermark that was present in the object store can be used in the flow.
     */
    @Test
    public void pollUsingWatermark() throws Exception
    {
        getDafultObjectStore().store("test", "testValue");
        executePollOf("usingWatermarkFlow");
        Thread.sleep(2000);
        ObjectStore os = getDafultObjectStore();
        assertTrue(os.contains("test"));
        assertEquals("keyPresent", os.retrieve("test"));
        assertNotSame(-1, registeredValues.indexOf("testValue"));
    }

    /**
     * Pre: The key is an expression
     * Pos: The key is evaluated twice, at the beginning of the message source and at the end of the flow
     */
    @Test
    public void watermarkWithKeyAsAnExpression() throws Exception
    {
        getDafultObjectStore().store("test", "testValue");
        executePollOf("watermarkWithKeyAsAnExpression");
        Thread.sleep(2000);
        ObjectStore os = getDafultObjectStore();
        assertTrue(os.contains("test"));
        assertEquals("keyPresent", os.retrieve("test"));
        assertNotSame(-1, registeredValues.indexOf("noKey"));
    }


    @Test
    public void validateNotificationFiring() throws Exception
    {
        muleContext.registerListener(new WatermarkNotificationListener());
        getDafultObjectStore().store("test", "testValue");
        executePollOf("watermarkWithKeyAsAnExpression");
        Thread.sleep(2000);
        ObjectStore os = getDafultObjectStore();
        assertTrue(os.contains("test"));
        assertEquals("keyPresent", os.retrieve("test"));
        assertNotSame(-1, registeredValues.indexOf("noKey"));

        assertEquals(WatermarkAction.WATERMARK_RETRIEVED_ACTION_NAME, registeredNotification.get(0).getName());
        assertEquals(WatermarkAction.WATERMARK_STORED_ATTRIBUTE_NAME, registeredNotification.get(1).getName());

    }

    private ObjectStore getDafultObjectStore()
    {
        return muleContext.getRegistry().lookupObject(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME);
    }

    private void executePollOf(String flowName) throws Exception
    {
        MessageProcessorPollingConnector connector = muleContext.getRegistry().lookupObject("connector.polling.mule.default");
        AbstractPollingMessageReceiver messageReceiver = (AbstractPollingMessageReceiver) connector.lookupReceiver(flowName + "~");
        messageReceiver.performPoll();
    }

    public static class FooComponent
    {

        public boolean process(String s)
        {
            registeredValues.add(s);
            return true;
        }
    }

    public class WatermarkNotificationListener implements CustomNotificationListener<CustomMetadataNotification>
    {

        @Override
        public void onNotification(ServerNotification notification)
        {
            registeredNotification.add((CustomMetadataNotification) notification);
        }
    }

    public static class PollStopper implements MuleContextAware
    {


        @Override
        public void setMuleContext(MuleContext context)
        {
            Map<String, MessageProcessorPollingConnector> connectors
                    = context.getRegistry().lookupByType(MessageProcessorPollingConnector.class);

            for (MessageProcessorPollingConnector connector : connectors.values())
            {
                connector.setInitialStateStopped(true);
            }
        }


    }
}
