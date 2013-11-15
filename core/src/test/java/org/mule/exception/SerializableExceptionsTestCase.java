/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.exception;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.store.ObjectStore;
import org.mule.api.transport.Connectable;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.ConnectException;

import java.io.Serializable;

import org.junit.Test;

public class SerializableExceptionsTestCase extends AbstractMuleContextTestCase implements Serializable
{

    private static final long serialVersionUID = 7564400529592600241L;

    private static final String key = "key";
    private static final String message = "a message";
    private static final String value = "Hello world!";

    private transient ObjectStore<MuleException> os;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        this.os = muleContext.getObjectStoreManager().getObjectStore("serializableExceptions", true);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        this.os.clear();
        super.doTearDown();
    }

    @Test
    public void testSerializableConnectException() throws Exception
    {
        TestSerializableConnectable connectable = new TestSerializableConnectable();
        connectable.setValue(value);

        ConnectException e = new ConnectException(new Exception(message), connectable);
        this.os.store(key, e);

        e = (ConnectException) this.os.retrieve(key);

        assertTrue(e.getMessage().contains(message));
        Connectable failed = e.getFailed();
        assertNotNull("Connectable was not serialized", failed);
        assertTrue(failed instanceof TestSerializableConnectable);

        assertEquals(value, ((TestSerializableConnectable) failed).getValue());
    }

    @Test
    public void testNonSerializableConnectException() throws Exception
    {
        ConnectException e = new ConnectException(new Exception(message), new TestConnectable());
        this.os.store(key, e);

        e = (ConnectException) this.os.retrieve(key);

        assertTrue(e.getMessage().contains(message));
        Connectable failed = e.getFailed();
        assertNull(failed);
    }

    @Test
    public void testSerializableMessagingException() throws Exception
    {
        TestSerializableMessageProcessor processor = new TestSerializableMessageProcessor();
        processor.setValue(value);

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        this.os.store(key, e);
        e = (MessagingException) this.os.retrieve(key);

        assertTrue(e.getMessage().contains(message));
        assertNotNull(e.getFailingMessageProcessor());
        assertTrue(e.getFailingMessageProcessor() instanceof TestSerializableMessageProcessor);
        assertEquals(value, ((TestSerializableMessageProcessor) e.getFailingMessageProcessor()).getValue());
    }

    @Test
    public void testNonSerializableMessagingException() throws Exception
    {
        TestMessageProcessor processor = new TestMessageProcessor();

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        this.os.store(key, e);
        e = (MessagingException) this.os.retrieve(key);

        assertTrue(e.getMessage().contains(message));
        assertNull(e.getFailingMessageProcessor());
    }
}
