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

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.module.client.remoting.notification.RemoteDispatcherNotification;
import org.mule.module.xml.transformer.wire.XStreamWireFormat;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.transformer.wire.SerializationWireFormat;

import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class RemoteDispatcherSerializationTestCase extends AbstractMuleTestCase
{
    protected RemoteDispatcherNotification getNotification()
    {
        Map props = new HashMap();
        props.put("key1", "value1");
        Apple apple = new Apple();
        apple.wash();
        RemoteDispatcherNotification n = new RemoteDispatcherNotification(new DefaultMuleMessage(apple, props), RemoteDispatcherNotification.ACTION_SEND, "vm://foo");
        n.setProperty("foo", "bar");
        return n;
    }
    public void testNotificationJavaSerialization() throws Exception
    {
        doTestNotificationSerialization(new SerializationWireFormat());
    }

//    public void testNotificationXmlSerialization() throws Exception
//    {
//        XStreamWireFormat wf = new XStreamWireFormat();
//        wf.setTransferObjectClass(RemoteDispatcherNotification.class);
//        doTestNotificationSerialization(wf);
//    }

    public void doTestNotificationSerialization(WireFormat wf) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wf.write(baos, getNotification(), "UTF-8");

        System.out.println(new String(baos.toByteArray()));
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
