/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;


import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.module.extension.internal.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;

/**
 * {@link SoapComponentWrapper} implementation for classes that implements the {@link SoapServiceProvider} interface.
 *
 * @since 4.0
 */
public class SoapServiceProviderWrapper extends SoapComponentWrapper implements ParameterizableTypeElement {

  SoapServiceProviderWrapper(Class<? extends SoapServiceProvider> aClass) {
    super(aClass);
  }

  /**
   * @return a {@link Class} that implements the {@link SoapServiceProvider} interface which this {@link Type} represents.
   */
  @Override
  public Class<? extends SoapServiceProvider> getDeclaringClass() {
    return (Class<? extends SoapServiceProvider>) super.getDeclaringClass();
  }

  @Override
  public String getAlias() {
    String finalName = hyphenize(super.getAlias())
        .replace("-service-provider", "")
        .replace("-connection", "")
        .replace("-extension", "");
    return finalName + "-" + DEFAULT_CONNECTION_PROVIDER_NAME;
  }
}
