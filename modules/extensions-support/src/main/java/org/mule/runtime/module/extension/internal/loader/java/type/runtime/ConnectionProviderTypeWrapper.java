/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;

/**
 * {@link TypeWrapper} specification for classes that are considered as Connection Providers
 *
 * @since 4.0
 */
public class ConnectionProviderTypeWrapper<T> extends TypeWrapper
    implements ConnectionProviderElement, ParameterizableTypeElement {

  public ConnectionProviderTypeWrapper(Class<T> aClass, ClassTypeLoader classTypeLoader) {
    super(aClass, classTypeLoader);
  }
}
