/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.serialization.internal;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.serialization.SerializationException;
import org.mule.util.SerializationUtils;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Implementation of {@link org.mule.api.serialization.ObjectSerializer} that uses Java's default serialization
 * mechanism. This means that exceptions will come from serializing objects that do
 * not implement {@link Serializable}
 *
 * @since 3.7.0
 */
public class JavaObjectSerializer extends AbstractObjectSerializer
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] doSerialize(Object object) throws Exception
    {
        if (object != null && !(object instanceof Serializable))
        {
            throw new SerializationException(String.format(
                    "Was expecting a Serializable type. %s was found instead", object.getClass()
                            .getCanonicalName()));
        }

        if (object != null && !(object instanceof Serializable))
        {
            throw new SerializationException(String.format(
                    "Was expecting a Serializable type. %s was found instead", object.getClass()
                            .getName()));
        }

        return SerializationUtils.serialize((Serializable) object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> T doDeserialize(InputStream inputStream, ClassLoader classLoader) throws Exception
    {
        checkArgument(inputStream != null, "Cannot deserialize a null stream");
        checkArgument(classLoader != null, "Cannot deserialize with a null classloader");

        return (T) SerializationUtils.deserialize(inputStream, classLoader, muleContext);
    }

    @Override
    protected <T> T postInitialize(T object)
    {
        //does nothing since SerializationUtils already does this on its own
        return object;
    }
}
