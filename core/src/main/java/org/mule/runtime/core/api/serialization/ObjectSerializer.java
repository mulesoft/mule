/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.serialization;

/**
 * Provides access to the different {@link SerializationProtocol} used in the container.
 */
public interface ObjectSerializer {

  /**
   * Provides access to the serialization protocol used for internal consumption, that is, when objects are serialized
   * on the container to reuse them later inside the container.
   * <p/>
   * Internal serialization uses a custom serialization protocol intended to overcome the limitations imposed by the classloading isolation.
   * This custom protocol requires of the artifact classloader structure defined at runtime in order to deserialize objects.
   *
   * @return the internal serialization protocol
   */
  SerializationProtocol getInternalProtocol();

  /**
   * Provides access to the serialization protocol used for external consumption, that is, when objects are serialized
   * on the container to be consumed later outside the container.
   * <p/>
   * External serialization does not requires knowledge about the artifact classloader structure at runtime. Every serialized object's class
   * is assumed to be available on the consumer side.
   *
   * @return the external serialization protocol
   */
  SerializationProtocol getExternalProtocol();
}
