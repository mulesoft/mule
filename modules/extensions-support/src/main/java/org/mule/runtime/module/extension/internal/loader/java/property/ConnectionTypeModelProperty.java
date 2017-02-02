/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;

/**
 * A {@link ModelProperty} meant to be used on {@link ConnectionProviderModel connection provider models},
 * which indicates the type of the produced connections
 *
 * @since 4.0
 */
public final class ConnectionTypeModelProperty implements ModelProperty {

  private final Class<?> connectionType;

  /**
   * Creates a new instance
   *
   * @param connectionType a connection type
   */
  public ConnectionTypeModelProperty(Class<?> connectionType) {
    this.connectionType = connectionType;
  }

  /**
   * @return a connection type
   */
  public Class<?> getConnectionType() {
    return connectionType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "connectionType";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
