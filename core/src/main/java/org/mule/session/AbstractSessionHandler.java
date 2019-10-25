/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.util.Collections.unmodifiableSet;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.serialization.DefaultObjectSerializer;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.serialization.SerializationException;
import org.mule.api.transport.SessionHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.serialization.internal.ClassSpecificObjectSerializer;
import org.mule.serialization.internal.MuleSessionWithNativeTypesSerializer;
import org.mule.util.store.DeserializationPostInitialisable;

/**
 * Base class for implementations of {@link SessionHandler}
 * which adds common functionality, specially around the concept
 * of serialization
 *
 * @since 3.7.0
 */
public abstract class AbstractSessionHandler implements SessionHandler
{
    public static final String ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY = SYSTEM_PROPERTY_PREFIX
            + "session.serialization.native.enable";

    protected boolean ACTIVATE_NATIVE_SESSION_SERIALIZATION = Boolean
            .getBoolean(ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY);


    private ObjectSerializer nativeObjectSerializer = new MuleSessionWithNativeTypesSerializer();
    private ObjectSerializerLocator objectSerializerLocator = new FromMessageObjectSerializerLocator();

    private ObjectSerializer wrappedSerializer = null;

    protected <T> T deserialize(MuleMessage message, byte[] bytes)
    {
        return deserialize(message, bytes, nativeObjectSerializer);
    }

    private boolean isNativeSerializationActivated(String endpoint)
    {
        return ACTIVATE_NATIVE_SESSION_SERIALIZATION;
    }

    private <T> T deserialize(MuleMessage message, byte[] bytes, ObjectSerializer objectSerializer)
    {
        T object;
        if (isNativeSerializationActivated(getEndpoint(message)))
        {
            object = nativeObjectSerializer.deserialize(bytes, message.getMuleContext().getExecutionClassLoader());
        }
        else
        {
            object = objectSerializerLocator.getObjectSerializer(message).deserialize(bytes, message.getMuleContext().getExecutionClassLoader());
        }

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

    protected byte[] serialize(MuleMessage message, Object object)
    {
        if (isNativeSerializationActivated(getEndpoint(message)))
        {
            return nativeObjectSerializer.serialize(object);
        }
        else
        {
            return objectSerializerLocator.getObjectSerializer(message).serialize(object);
        }
    }

    protected String getEndpoint(MuleMessage message)
    {
        return valueOf(message.getInboundProperty("MULE_ENDPOINT"));
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
