/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;

public abstract class SerializationTestUtils {

  private static final String key = "SerializationTestComponentKey";

  public static <T extends Exception> T testException(T exception, MuleContext muleContext) {
    ObjectStore<T> os = getObjectStore(muleContext);
    try {
      os.store(key, exception);
      return os.retrieve(key);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        os.clear();
      } catch (ObjectStoreException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static <T extends Exception> ObjectStore<T> getObjectStore(MuleContext muleContext) {
    return muleContext.getObjectStoreManager().createObjectStore("SerializationTestUtils",
                                                                 ObjectStoreSettings.builder().persistent(true).build());
  }

  public static ObjectSerializer getJavaSerializerWithMockContext() {
    MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    when(muleContext.getExecutionClassLoader()).thenReturn(ObjectSerializer.class.getClassLoader());
    return getJavaSerializer(muleContext);
  }

  public static ObjectSerializer addJavaSerializerToMockMuleContext(MuleContext mockMuleContext) {
    ObjectSerializer objectSerializer = getJavaSerializer(mockMuleContext);
    when(mockMuleContext.getObjectSerializer()).thenReturn(objectSerializer);
    return objectSerializer;
  }

  private static ObjectSerializer getJavaSerializer(MuleContext muleContext) {
    JavaObjectSerializer serializer = new JavaObjectSerializer();
    serializer.setMuleContext(muleContext);

    return serializer;
  }

}
