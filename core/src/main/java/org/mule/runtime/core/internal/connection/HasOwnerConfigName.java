/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;

import java.util.Optional;

/**
 * Interface to be used in {@link ConnectionProvider} indicating that the marked provider knows the name of its owner
 * configuration.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface HasOwnerConfigName {

  /**
   * @return The name of the config that owns this {@link ConnectionProvider}.
   */
  Optional<String> getOwnerConfigName();
}
