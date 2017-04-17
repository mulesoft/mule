/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import static java.util.Arrays.stream;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.annotation.Soap;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * {@link SoapComponentWrapper} implementation for the {@link Extension} annotated class, which is a Soap Extension either
 * because implements the {@link SoapServiceProvider} interface or is annotated with the {@link Soap} annotation.
 *
 * @since 4.0
 */
public class SoapExtensionTypeWrapper<T> extends SoapComponentWrapper {

  public SoapExtensionTypeWrapper(Class<T> extensionType) {
    super(extensionType);
  }

  public List<SoapServiceProviderWrapper> getSoapServiceProviders() {
    ImmutableList.Builder<SoapServiceProviderWrapper> serviceProviders = ImmutableList.builder();
    if (SoapServiceProvider.class.isAssignableFrom(this.getDeclaringClass())) {
      serviceProviders.add(new SoapServiceProviderWrapper((Class<? extends SoapServiceProvider>) this.getDeclaringClass()));
    }
    this.getAnnotation(Soap.class)
        .ifPresent(soap -> stream(soap.value()).forEach(sp -> serviceProviders.add(new SoapServiceProviderWrapper(sp))));
    return serviceProviders.build();
  }
}
