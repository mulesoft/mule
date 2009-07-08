/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.module.client.remoting.notification.RemoteDispatcherNotification;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.wire.SerializationWireFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class RemoteDispatcherSerializationTestCase extends AbstractMuleTestCase
{
    protected RemoteDispatcherNotification getNotification()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put("key1", "value1");
        
        Apple apple = new Apple();
        apple.wash();
        
        MuleMessage message = new DefaultMuleMessage(apple, props, muleContext);
        RemoteDispatcherNotification notification = new RemoteDispatcherNotification(message,
            RemoteDispatcherNotification.ACTION_SEND, "vm://foo");
        notification.setProperty("foo", "bar");
        return notification;
    }
    
    public void testNotificationJavaSerialization() throws Exception
    {
        doTestNotificationSerialization(new SerializationWireFormat());
    }

    // TODO MULE-3118
//    public void testNotificationXmlSerialization() throws Exception
//    {
//        XStreamWireFormat wireFormat = new XStreamWireFormat();
//        wireFormat.setMuleContext(muleContext);
//        wireFormat.setTransferObjectClass(RemoteDispatcherNotification.class);
//        
//        doTestNotificationSerialization(wireFormat);
//    }

    public void doTestNotificationSerialization(WireFormat wf) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wf.write(baos, getNotification(), "UTF-8");

        Object result = wf.read(new ByteArrayInputStream(baos.toByteArray()));

        assertNotNull(result);
        assertTrue(result instanceof RemoteDispatcherNotification);
        doTestNotification((RemoteDispatcherNotification)result);
    }

    protected void doTestNotification(RemoteDispatcherNotification n) throws Exception
    {
        assertEquals("bar", n.getProperty("foo"));
        MuleMessage m = n.getMessage();
        assertTrue(m.getPayload() instanceof Apple);
        assertTrue(((Apple)m.getPayload()).isWashed());
        assertEquals("value1", m.getProperty("key1"));

    }
}
