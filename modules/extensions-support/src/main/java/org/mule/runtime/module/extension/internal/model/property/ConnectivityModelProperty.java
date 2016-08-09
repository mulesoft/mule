/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;


import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ModelProperty;

/**
 * An immutable model property which specifies that the owning {@link EnrichableModel} requires a connection of a given
 * {@link #connectionType}
 *
 * @since 4.0
 */
public final class ConnectivityModelProperty implements ModelProperty {

  private final MetadataType connectionType;
  private final boolean supportsTransactions;

  /**
   * Creates a new instance for the given {@code connectionType}
   *
   * @param connectionType
   */
  public ConnectivityModelProperty(MetadataType connectionType) {
    this.connectionType = connectionType;
    this.supportsTransactions = TransactionalConnection.class.isAssignableFrom(getType(connectionType));
  }

  /**
   * @return the {@link {@link #connectionType}}
   */
  public MetadataType getConnectionType() {
    return connectionType;
  }

  /**
   * @return whether this connection supports transactions
   */
  public boolean supportsTransactions() {
    return supportsTransactions;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code connectionType}
   */
  @Override
  public String getName() {
    return "connectionType";
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isExternalizable() {
    return false;
  }
}
