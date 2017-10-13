/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.internal.value.MuleValueProviderServiceUtility.deleteLastPartFromLocation;
import static org.mule.runtime.core.internal.value.MuleValueProviderServiceUtility.isConnection;
import static org.mule.runtime.extension.api.values.ValueResolvingException.INVALID_LOCATION;
import static org.mule.runtime.extension.api.values.ValueResolvingException.UNKNOWN;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.internal.dsl.model.NoSuchComponentModelException;
import org.mule.runtime.extension.api.values.ComponentValueProvider;
import org.mule.runtime.extension.api.values.ConfigurationParameterValueProvider;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link ValueProviderService} implementation flavour that initialises just the required components before executing the
 * resolving logic.
 * <p>
 * This guarantees that if the application has been created lazily, the requested components exists before the execution of the
 * actual {@link ValueProviderService}.
 *
 * @since 4.0
 * @see ValueProviderService
 */
public class LazyValueProviderService implements ValueProviderService, Initialisable {

  public static final String NON_LAZY_VALUE_PROVIDER_SERVICE = "_muleNonLazyValueProviderService";
  private final Supplier<ValueProviderService> valueProviderServiceSupplier;
  private Supplier<ConfigurationComponentLocator> componentLocatorSupplier;

  private LazyComponentInitializer lazyComponentInitializer;

  @Inject
  @Named(NON_LAZY_VALUE_PROVIDER_SERVICE)
  private ValueProviderService providerService;

  private ConfigurationComponentLocator componentLocator;

  LazyValueProviderService(LazyComponentInitializer lazyComponentInitializer,
                           Supplier<ValueProviderService> valueProviderServiceSupplier,
                           Supplier<ConfigurationComponentLocator> componentLocatorSupplier) {
    this.lazyComponentInitializer = lazyComponentInitializer;
    this.valueProviderServiceSupplier = valueProviderServiceSupplier;
    this.componentLocatorSupplier = componentLocatorSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValueResult getValues(Location location, String providerName) {
    return initializeComponent(location)
        .orElseGet(() -> providerService.getValues(location, providerName));
  }

  private Optional<ValueResult> initializeComponent(Location location) {
    Location locationWithOutConnection = locationWithOutConnection(location);

    try {
      lazyComponentInitializer.initializeComponent(locationWithOutConnection);
    } catch (Exception e) {
      Throwable rootException = getRootException(e);
      if (rootException instanceof NoSuchComponentModelException) {
        return of(resultFrom(newFailure(e)
            .withFailureCode(INVALID_LOCATION)
            .withMessage(format("Unable to resolve values. No component was found in the given location [%s]", location))
            .build()));
      }

      return of(resultFrom(newFailure(e)
          .withMessage("Unknown error occurred trying to resolve values. " + e.getMessage())
          .withFailureCode(UNKNOWN)
          .build()));
    }

    return empty();
  }

  private Location locationWithOutConnection(Location location) {
    return isConnection(location) ? deleteLastPartFromLocation(location) : location;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.providerService = valueProviderServiceSupplier.get();
    this.componentLocator = componentLocatorSupplier.get();
  }

  public Optional<ValueProviderModel> getModel(Location location, String providerName) {
    boolean isConnection = isConnection(location);

    if (isConnection) {
      location = deleteLastPartFromLocation(location);
    }

    return getModel(location, isConnection, providerName);
  }

  private Optional<ValueProviderModel> getModel(Location location, boolean isConnection, String providerName) {
    Reference<ValueProviderModel> model = new Reference<>();

    componentLocator.find(location).ifPresent(provider -> {
      try {
        if (provider instanceof ComponentValueProvider) {
          model.set(((ComponentValueProvider) provider).getModels(providerName).get(0));
        } else if (provider instanceof ConfigurationParameterValueProvider) {
          if (isConnection) {
            model.set(((ConfigurationParameterValueProvider) provider).getConnectionModels(providerName).get(0));
          } else {
            model.set(((ConfigurationParameterValueProvider) provider).getConfigModels(providerName).get(0));
          }
        }
      } catch (Exception ignored) {
      }
    });

    return ofNullable(model.get());
  }
}
