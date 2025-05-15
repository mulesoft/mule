/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.bean;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.serialization.ObjectSerializer;

/**
 * Wrapper for an {@link ObjectSerializer} which can be updated once the artifact configuration is loaded and applied.
 *
 * @since 4.10
 */
@NoImplement
public interface ObjectSerializerDelegate extends ObjectSerializer {

  /**
   * Changes the inner delegate of this serializer.
   *
   * @param delegate the new serializer to delegate to.
   */
  void setDelegate(ObjectSerializer delegate);

  /**
   * @return the serializer to delegate to.
   */
  ObjectSerializer getDelegate();

}
