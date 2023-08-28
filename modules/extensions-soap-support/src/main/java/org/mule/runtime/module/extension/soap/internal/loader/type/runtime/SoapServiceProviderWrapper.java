/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;


import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;

import java.util.Optional;

/**
 * {@link SoapComponentWrapper} implementation for classes that implements the {@link SoapServiceProvider} interface.
 *
 * @since 4.0
 */
public class SoapServiceProviderWrapper extends SoapComponentWrapper implements ParameterizableTypeElement {

  SoapServiceProviderWrapper(Class<? extends SoapServiceProvider> aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
  }

  /**
   * @return a {@link Class} that implements the {@link SoapServiceProvider} interface which this {@link Type} represents.
   */
  @Override
  public Optional<Class<?>> getDeclaringClass() {
    return super.getDeclaringClass();
  }

  @Override
  public String getAlias() {
    String finalName = hyphenize(super.getAlias())
        .replace("-service-provider", "")
        .replace("-connection", "")
        .replace("-extension", "");
    return finalName + "-" + DEFAULT_CONNECTION_PROVIDER_NAME;
  }

  public boolean supportsConnectivityTesting() {
    return super.getDeclaringClass()
        .map(clazz -> !NoConnectivityTest.class.isAssignableFrom(clazz)
            && !org.mule.sdk.api.connectivity.NoConnectivityTest.class.isAssignableFrom(clazz))
        .orElse(true);
  }
}
