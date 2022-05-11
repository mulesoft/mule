/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.lang.ref.WeakReference;

/**
 * A {@link ModelProperty} meant to be used on {@link ConnectionProviderModel connection provider models}, which indicates the
 * type of the produced connections
 *
 * @since 4.0
 */
public final class ConnectionTypeModelProperty implements ModelProperty {

  private WeakReference<Class<?>> connectionType;
  private Type type;

  /**
   * Creates a new instance
   *
   * @param connectionType a connection type
   */
  public ConnectionTypeModelProperty(Class<?> connectionType) {
    this.connectionType = new WeakReference<>(connectionType);
    this.type = new TypeWrapper(connectionType, new DefaultExtensionsTypeLoaderFactory().createTypeLoader());
  }

  public ConnectionTypeModelProperty(Type type) {
    this.type = type;
  }

  /**
   * @return a connection type
   */
  public Class<?> getConnectionType() {
    return connectionType.get();
  }

  public Type getConnectionTypeElement() {
    return type;
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
