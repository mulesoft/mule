/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
