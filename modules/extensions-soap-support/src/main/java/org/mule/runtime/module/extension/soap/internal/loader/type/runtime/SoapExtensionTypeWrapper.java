/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.annotation.Soap;
import org.mule.runtime.extension.api.soap.annotation.SoapMessageDispatcherProviders;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

/**
 * {@link SoapComponentWrapper} implementation for the {@link Extension} annotated class, which is a Soap Extension either because
 * implements the {@link SoapServiceProvider} interface or is annotated with the {@link Soap} annotation.
 *
 * @since 4.0
 */
public class SoapExtensionTypeWrapper<T> extends SoapComponentWrapper {

  private ClassTypeLoader typeLoader;

  public SoapExtensionTypeWrapper(Class<T> extensionType, ClassTypeLoader typeLoader) {
    super(extensionType, typeLoader);
    this.typeLoader = typeLoader;
  }

  public List<SoapServiceProviderWrapper> getSoapServiceProviders() {
    ImmutableList.Builder<SoapServiceProviderWrapper> serviceProviders = ImmutableList.builder();
    if (this.isAssignableTo(SoapServiceProvider.class)) {
      serviceProviders
          .add(new SoapServiceProviderWrapper((Class<? extends SoapServiceProvider>) this.getDeclaringClass().get(), typeLoader));
    }
    this.getAnnotation(Soap.class).ifPresent(soap -> stream(soap.value())
        .forEach(sp -> serviceProviders.add(new SoapServiceProviderWrapper(sp, typeLoader))));
    return serviceProviders.build();
  }

  public List<MessageDispatcherProviderTypeWrapper> getDispatcherProviders() {
    ImmutableList.Builder<MessageDispatcherProviderTypeWrapper> transportProviders = ImmutableList.builder();
    Optional<SoapMessageDispatcherProviders> customTransport = this.getAnnotation(SoapMessageDispatcherProviders.class);
    customTransport.ifPresent(ct -> transportProviders.addAll(stream(ct.value())
        .map(providerClass -> new MessageDispatcherProviderTypeWrapper(providerClass, typeLoader))
        .collect(toList())));
    return transportProviders.build();
  }
}
