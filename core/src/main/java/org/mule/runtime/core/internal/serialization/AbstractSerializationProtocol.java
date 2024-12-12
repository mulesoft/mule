/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.serialization;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.serialization.SerializationException;
import org.mule.runtime.api.serialization.SerializationProtocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base class for implementations of {@link SerializationProtocol} This class implements all the base behavioral contract allowing
 * its extensions to only care about the actual serialization/deserialization part.
 */
public abstract class AbstractSerializationProtocol implements SerializationProtocol {

  /**
   * Serializes the given object. Should not care about error handling
   *
   * @param object the object to be serialized
   * @return an array of bytes
   * @throws Exception any exception thrown. Base class will handle accordingly
   */
  protected abstract byte[] doSerialize(Object object) throws Exception;

  /**
   * Deserializes the given {@code inputStream} using the provided {@code classLoader}. No need to worry about error handling or
   * deserialization post initialization. Base class does all of that automatically
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
  public byte[] serialize(Object object) throws SerializationException {
    try {
      return doSerialize(object);
    } catch (Exception e) {
      throw new SerializationException("Could not serialize object", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if object is not a {@link java.io.Serializable}
   */
  @Override
  public void serialize(Object object, OutputStream out) throws SerializationException {
    try {
      byte[] bytes = serialize(object);
      out.write(bytes);
      out.flush();
    } catch (IOException e) {
      throw new SerializationException("Could not write to output stream", e);
    } finally {
      closeQuietly(out);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(byte[] bytes) throws SerializationException {
    return deserialize(bytes, getExecutionClassLoader());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(byte[] bytes, ClassLoader classLoader) throws SerializationException {
    requireNonNull(bytes, "The byte[] must not be null");
    return deserialize(new ByteArrayInputStream(bytes), classLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T deserialize(InputStream inputStream) throws SerializationException {
    return deserialize(inputStream, getExecutionClassLoader());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T deserialize(InputStream inputStream, ClassLoader classLoader) throws SerializationException {
    requireNonNull(inputStream, "Cannot deserialize a null stream");
    requireNonNull(classLoader, "Cannot deserialize with a null classloader");
    try {
      return (T) doDeserialize(inputStream, classLoader);
    } catch (Exception e) {
      throw new SerializationException("Could not deserialize object", e);
    } finally {
      closeQuietly(inputStream);
    }
  }

  protected abstract ClassLoader getExecutionClassLoader();

}
