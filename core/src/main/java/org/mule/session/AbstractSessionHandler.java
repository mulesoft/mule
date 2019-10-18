/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.serialization.DefaultObjectSerializer;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.serialization.SerializationException;
import org.mule.api.transport.SessionHandler;
import org.mule.serialization.internal.ClassSpecificObjectSerializer;
import org.mule.util.store.DeserializationPostInitialisable;

import javax.inject.Inject;

/**
 * Base class for implementations of {@link SessionHandler}
 * which adds common functionality, specially around the concept
 * of serialization
 *
 * @since 3.7.0
 */
public abstract class AbstractSessionHandler implements SessionHandler
{
    private ObjectSerializerLocator objectSerializerLocator = new FromMessageObjectSerializerLocator();

    protected <T> T deserialize(MuleMessage message, byte[] bytes)
    {
        ObjectSerializer objectSerializer = serSerializerClass(objectSerializerLocator.getObjectSerializer(message), MuleSession.class);
        T object = objectSerializer.deserialize(bytes, message.getMuleContext().getExecutionClassLoader());
        if (object instanceof DeserializationPostInitialisable)
        {
            try
            {
                DeserializationPostInitialisable.Implementation.init(object, message.getMuleContext());
            }
            catch (Exception e)
            {
                throw new SerializationException("Could not initialise session after deserialization", e);
            }
        }

        return object;
    }

    private ObjectSerializer serSerializerClass(ObjectSerializer serializer, Class clazz)
    {
        if (serializer instanceof ClassSpecificObjectSerializer)
        {
            return serializer;
        }
        else
        {
            return new ClassSpecificObjectSerializer(serializer, clazz);
        }
    }

    protected byte[] serialize(MuleMessage message, Object object)
    {
        return objectSerializerLocator.getObjectSerializer(message).serialize(object);
    }

    @Inject
    @DefaultObjectSerializer
    public void setObjectSerializer(ObjectSerializer objectSerializer)
    {
        objectSerializerLocator = new FixedObjectSerializerLocator(objectSerializer);
    }

    private interface ObjectSerializerLocator
    {

        ObjectSerializer getObjectSerializer(MuleMessage message);
    }

    private class FixedObjectSerializerLocator implements ObjectSerializerLocator
    {
        private final ObjectSerializer objectSerializer;

        private FixedObjectSerializerLocator(ObjectSerializer objectSerializer)
        {
            this.objectSerializer = objectSerializer;
        }

        @Override
        public ObjectSerializer getObjectSerializer(MuleMessage message)
        {
            return objectSerializer;
        }
    }

    private class FromMessageObjectSerializerLocator implements ObjectSerializerLocator
    {
        @Override
        public ObjectSerializer getObjectSerializer(MuleMessage message)
        {
            return message.getMuleContext().getObjectSerializer();
        }
    }
}
