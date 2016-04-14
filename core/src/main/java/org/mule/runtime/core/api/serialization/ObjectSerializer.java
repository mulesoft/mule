/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.serialization;

import org.mule.util.store.DeserializationPostInitialisable;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Defines a component capable to serialize/deserialize objects into/from an array of
 * {@link byte}s. Unlike usual serializing components, this one doesn't enforce the
 * serialized object to implement {@link Serializable}. However, some implementations
 * might require that condition and throw {@link IllegalArgumentException} if not
 * met.
 * <p/>
 * Implementations are also responsible for the correct initialization of classes
 * implementing the {@link DeserializationPostInitialisable}
 * interface.
 * <p/>
 * <p/>
 * Unexpected behavior can result of deserializing objects that were generated with
 * a different implementation of {@link ObjectSerializer}.
 * <p/>
 * Implementations are required to be thread-safe
 *
 * @since 3.7.0
 */
public interface ObjectSerializer
{

    /**
     * Serializes the given object into a an array of {@link byte}s
     *
     * @param object the object to be serialized. Might be <code>null</code>
     * @return an array of {@link byte}
     * @throws SerializationException in case of unexpected exception
     */
    byte[] serialize(Object object) throws SerializationException;

    /**
     * Serializes the given object and writes the result into {@code out}
     *
     * @param object the object to be serialized. Might be <code>null</code>
     * @param out    an {@link OutputStream} where the result will be written
     * @return an array of {@link byte}
     * @throws SerializationException in case of unexpected exception
     */
    void serialize(Object object, OutputStream out) throws SerializationException;

    /**
     * Deserializes the given bytes. Unexpected behavior can result of deserializing
     * a byte[] that was generated with another implementation.
     * <p/>
     * If the obtained object implements {@link DeserializationPostInitialisable}
     * then this serializer will be responsible for properly initializing
     * the object before returning it.
     * <p/>
     * Implementation will choose the {@link java.lang.ClassLoader}
     * to use for deserialization.
     *
     * @param bytes an array of byte that an original object was serialized into
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code bytes} is {@code null}
     * @throws SerializationException   in case of unexpected exception
     */
    <T> T deserialize(byte[] bytes) throws SerializationException;

    /**
     * Deserializes the given bytes.
     * <p/>
     * If the obtained object implements {@link DeserializationPostInitialisable}
     * then this serializer will be responsible for properly initializing
     * the object before returning it.
     *
     * @param bytes       an array of byte that an original object was serialized into
     * @param classLoader the {@link java.lang.ClassLoader} to deserialize with
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code bytes} is {@code null}
     * @throws SerializationException   in case of unexpected exception
     */
    <T> T deserialize(byte[] bytes, ClassLoader classLoader) throws SerializationException;

    /**
     * Deserializes the given stream of bytes.
     * <p/>
     * Implementation will choose the {@link java.lang.ClassLoader}
     * to use for deserialization.
     * <p/>
     * Even if deserialization fails, this method will close the
     * {@code inputStream}
     * <p/>
     * If the obtained object implements {@link DeserializationPostInitialisable}
     * then this serializer will be responsible for properly initializing
     * the object before returning it.
     *
     * @param inputStream a stream of bytes that an original object was serialized into
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code inputStream} is {@code null}
     * @throws SerializationException   in case of unexpected exception
     */
    <T> T deserialize(InputStream inputStream) throws SerializationException;

    /**
     * Deserializes the given stream of bytes.
     * <p/>
     * Even if deserialization fails, this method will close the
     * {@code inputStream}
     * <p/>
     * If the obtained object implements {@link DeserializationPostInitialisable}
     * then this serializer will be responsible for properly initializing
     * the object before returning it.
     *
     * @param inputStream a stream of bytes that an original object was serialized into
     * @param classLoader the {@link java.lang.ClassLoader} to deserialize with
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code inputStream} is {@code null}
     * @throws SerializationException   in case of unexpected exception
     */
    <T> T deserialize(InputStream inputStream, ClassLoader classLoader) throws SerializationException;
}
