/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.serialization.internal;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.serialization.SerializationException;
import org.mule.util.IOUtils;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base class for implementations of {@link org.mule.api.serialization.ObjectSerializer}
 * This class implements all the base behavioral contract allowing its extensions to
 * only care about the actual serialization/deserialization part.
 *
 * @since 3.7.0
 */
public abstract class AbstractObjectSerializer implements ObjectSerializer, MuleContextAware
{

    protected MuleContext muleContext;

    public AbstractObjectSerializer() {}

    AbstractObjectSerializer(ObjectSerializer other)
    {
        if (other instanceof AbstractObjectSerializer)
        {
            this.muleContext = ((AbstractObjectSerializer) other).muleContext;
        }
    }

    /**
     * Serializes the given object. Should not care about error handling
     *
     * @param object the object to be serialized
     * @return an array of bytes
     * @throws Exception any exception thrown. Base class will handle accordingly
     */
    protected abstract byte[] doSerialize(Object object) throws Exception;

    /**
     * Deserializes the given {@code inputStream} using the provided {@code classLoader}.
     * No need to worry about error handling or deserialization post initialization. Base class
     * does all of that automatically
     *
     * @param inputStream an open {@link java.io.InputStream}, not to be explicitly closed in this method
     * @param classLoader a {@link java.lang.ClassLoader}
     * @return a deserialized object
     */
    protected abstract <T> T doDeserialize(InputStream inputStream, ClassLoader classLoader) throws Exception;

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if object is not a {@link java.io.Serializable}
     */
    @Override
    public byte[] serialize(Object object) throws SerializationException
    {
        try
        {
            return doSerialize(object);
        }
        catch (Exception e)
        {
            throw new SerializationException("Could not serialize object", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if object is not a {@link java.io.Serializable}
     */
    @Override
    public void serialize(Object object, OutputStream out) throws SerializationException
    {
        try
        {
            byte[] bytes = serialize(object);
            out.write(bytes);
            out.flush();
        }
        catch (IOException e)
        {
            throw new SerializationException("Could not write to output stream", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T deserialize(byte[] bytes) throws SerializationException
    {
        return deserialize(bytes, muleContext.getExecutionClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T deserialize(byte[] bytes, ClassLoader classLoader) throws SerializationException
    {
        checkArgument(bytes != null, "The byte[] must not be null");
        ByteArrayInputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(bytes);
            return deserialize(inputStream, classLoader);
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(InputStream inputStream) throws SerializationException
    {
        return deserialize(inputStream, muleContext.getExecutionClassLoader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T deserialize(InputStream inputStream, ClassLoader classLoader) throws SerializationException
    {
        checkArgument(inputStream != null, "Cannot deserialize a null stream");
        checkArgument(classLoader != null, "Cannot deserialize with a null classloader");
        try
        {
            return (T) postInitialize(doDeserialize(inputStream, classLoader));
        }
        catch (Exception e)
        {
            throw new SerializationException("Could not deserialize object", e);
        }
    }

    protected <T> T postInitialize(T object) throws SerializationException
    {
        if (object instanceof DeserializationPostInitialisable)
        {
            try
            {
                DeserializationPostInitialisable.Implementation.init(object, muleContext);
            }
            catch (Exception e)
            {
                throw new SerializationException(String.format(
                        "Could not initialize instance of %s after deserialization", object.getClass()
                                .getName()), e);
            }
        }

        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

}
