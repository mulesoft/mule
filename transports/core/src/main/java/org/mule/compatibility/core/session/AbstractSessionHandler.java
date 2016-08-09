/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.session;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.serialization.DefaultObjectSerializer;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.message.SessionHandler;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import javax.inject.Inject;

/**
 * Base class for implementations of {@link SessionHandler} which adds common functionality, specially around the concept of
 * serialization
 *
 * @since 3.7.0
 */
public abstract class AbstractSessionHandler implements SessionHandler {

  private ObjectSerializerLocator objectSerializerLocator = new FromMessageObjectSerializerLocator();

  protected <T> T deserialize(MuleMessage message, byte[] bytes, MuleContext muleContext) {
    T object = objectSerializerLocator.getObjectSerializer(message, muleContext)
        .deserialize(bytes, muleContext.getExecutionClassLoader());
    if (object instanceof DeserializationPostInitialisable) {
      try {
        DeserializationPostInitialisable.Implementation.init(object, muleContext);
      } catch (Exception e) {
        throw new SerializationException("Could not initialise session after deserialization", e);
      }
    }

    return object;
  }

  protected byte[] serialize(MuleMessage message, Object object, MuleContext muleContext) {
    return objectSerializerLocator.getObjectSerializer(message, muleContext).serialize(object);
  }

  @Inject
  @DefaultObjectSerializer
  public void setObjectSerializer(ObjectSerializer objectSerializer) {
    objectSerializerLocator = new FixedObjectSerializerLocator(objectSerializer);
  }

  private interface ObjectSerializerLocator {

    ObjectSerializer getObjectSerializer(MuleMessage message, MuleContext muleContext);
  }

  private class FixedObjectSerializerLocator implements ObjectSerializerLocator {

    private final ObjectSerializer objectSerializer;

    private FixedObjectSerializerLocator(ObjectSerializer objectSerializer) {
      this.objectSerializer = objectSerializer;
    }

    @Override
    public ObjectSerializer getObjectSerializer(MuleMessage message, MuleContext muleContext) {
      return objectSerializer;
    }
  }

  private class FromMessageObjectSerializerLocator implements ObjectSerializerLocator {

    @Override
    public ObjectSerializer getObjectSerializer(MuleMessage message, MuleContext muleContext) {
      return muleContext.getObjectSerializer();
    }
  }
}
