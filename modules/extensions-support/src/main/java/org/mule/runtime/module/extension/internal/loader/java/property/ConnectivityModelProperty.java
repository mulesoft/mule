/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;


import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.Type;

/**
 * An immutable model property which specifies that the owning {@link EnrichableModel} requires a connection of a given
 * {@link #connectionType}
 *
 * @since 4.0
 */
public class ConnectivityModelProperty implements ModelProperty {

  private Class<?> connectionType;
  private Type type;

  /**
   * Creates a new instance for the given {@code connectionType}
   *
   * @param connectionType
   */
  public ConnectivityModelProperty(Class<?> connectionType) {
    this.connectionType = connectionType;
  }

  /**
   * Creates a new instance for the given {@code connectionType}
   *
   * @param connectionType
   */
  public ConnectivityModelProperty(Type connectionType) {
    this.type = connectionType;
  }

  /**
   * @return the {@link {@link #connectionType}}
   */
  public Type getConnectionType() {
    return type;
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
  public boolean isPublic() {
    return false;
  }
}
