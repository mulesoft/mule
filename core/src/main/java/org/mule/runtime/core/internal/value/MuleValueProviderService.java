/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.value;

import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.internal.util.LocationUtils.deleteLastPartFromLocation;
import static org.mule.runtime.core.internal.util.LocationUtils.isConnection;
import static org.mule.runtime.extension.api.values.ValueResolvingException.INVALID_LOCATION;
import static org.mule.runtime.extension.api.values.ValueResolvingException.NOT_VALUE_PROVIDER_ENABLED;

import static java.lang.String.format;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.values.ComponentValueProvider;
import org.mule.runtime.extension.api.values.ConfigurationParameterValueProvider;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

import jakarta.inject.Inject;

/**
 * Default implementation of the {@link ValueProviderService}, which provides the capability to resolve {@link Value values} for
 * any capable element in the application, using it's {@link Location}.
 * <p>
 * Requires the injection of the {@link MuleContext}, to be able to lookup the component inside the Mule App flows using the given
 * {@link Location}
 *
 * @since 4.0
 */
public class MuleValueProviderService implements ValueProviderService {

  @Inject
  private ConfigurationComponentLocator componentLocator;

  /**
   * {@inheritDoc}
   */
  @Override
  public ValueResult getValues(Location location, String providerName) {
    return getValueResult(() -> this.findValueProvider(location, providerName, null).resolve());
  }

  @Override
  public ValueResult getFieldValues(Location location, String parameterName, String targetSelector) {
    return getValueResult(() -> this.findValueProvider(location, parameterName, targetSelector).resolve());
  }

  /**
   * Executes the {@link Value} resolving logic and wraps the result into a {@link ValueResult}. In case that the resolving
   * finished
   *
   * @param valueSupplier supplier which encapsulates the {@link Value} resolution logic.
   * @return A {@link ValueResult} indicating providing the {@link Set} of {@link Value values} or the produced
   *         {@link ResolvingFailure failure}.
   */
  private ValueResult getValueResult(ValueResultSupplier valueSupplier) {
    ValueResult result;
    try {
      result = resultFrom(valueSupplier.get());
    } catch (ValueResolvingException e) {
      ResolvingFailure failure = newFailure(e)
          .withFailureCode(e.getFailureCode())
          .build();
      result = resultFrom(failure);
    } catch (Exception e) {
      result = resultFrom(newFailure(e).build());
    }
    return result;
  }

  @FunctionalInterface
  private interface ValueResultSupplier {

    Set<Value> get() throws Exception;
  }

  private ValueProvider findValueProvider(Location location, String parameterName, String targetSelector)
      throws ValueResolvingException {
    boolean isConnection = isConnection(location);

    Location realLocation = isConnection
        ? deleteLastPartFromLocation(location)
        : location;

    Object component = findComponent(realLocation);

    if (component instanceof ComponentValueProvider) {
      if (targetSelector != null) {
        return () -> ((ComponentValueProvider) component).getValues(parameterName, targetSelector);
      } else {
        return () -> ((ComponentValueProvider) component).getValues(parameterName);
      }
    }

    if (component instanceof ConfigurationParameterValueProvider) {
      if (isConnection) {
        if (targetSelector != null) {
          return () -> ((ConfigurationParameterValueProvider) component).getConnectionValues(parameterName, targetSelector);
        } else {
          return () -> ((ConfigurationParameterValueProvider) component).getConnectionValues(parameterName);
        }
      } else {
        if (targetSelector != null) {
          return () -> ((ConfigurationParameterValueProvider) component).getConfigValues(parameterName, targetSelector);
        } else {
          return () -> ((ConfigurationParameterValueProvider) component).getConfigValues(parameterName);
        }
      }
    }

    throw new ValueResolvingException(format("The found element in the Location [%s] is not capable of provide Values",
                                             location),
                                      NOT_VALUE_PROVIDER_ENABLED);
  }

  private Object findComponent(Location location) throws ValueResolvingException {
    return componentLocator.find(location)
        .orElseThrow(() -> new ValueResolvingException(format("Invalid location [%s]. No element found in the given location.",
                                                              location),
                                                       INVALID_LOCATION));
  }
}
