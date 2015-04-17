/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.serialization.internal;

import org.mule.api.serialization.DefaultObjectSerializer;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.serialization.SerializationException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A wrapper which allows using an {@link ObjectSerializer} as the default
 * injection candidate. This is so because this wrapper is annotated
 * with {@link DefaultObjectSerializer}. This is only necessary because of
 * <a href="https://jira.spring.io/browse/SPR-12914">
 * Spring issue SPR-12914</a>. When that issue is fixed, then evaluate if
 * it makes sense to continue having this class
 *
 * @since 3.7.0
 */
@DefaultObjectSerializer
public class DefaultObjectSerializerWrapper implements ObjectSerializer
{

    private final ObjectSerializer delegate;

    public DefaultObjectSerializerWrapper(ObjectSerializer delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException
    {
        return delegate.serialize(object);
    }

    @Override
    public void serialize(Object object, OutputStream out) throws SerializationException
    {
        delegate.serialize(object, out);
    }

    @Override
    public <T> T deserialize(byte[] bytes) throws SerializationException
    {
        return delegate.deserialize(bytes);
    }

    @Override
    public <T> T deserialize(byte[] bytes, ClassLoader classLoader) throws SerializationException
    {
        return delegate.deserialize(bytes, classLoader);
    }

    @Override
    public <T> T deserialize(InputStream inputStream) throws SerializationException
    {
        return delegate.deserialize(inputStream);
    }

    @Override
    public <T> T deserialize(InputStream inputStream, ClassLoader classLoader) throws SerializationException
    {
        return delegate.deserialize(inputStream, classLoader);
    }
}
