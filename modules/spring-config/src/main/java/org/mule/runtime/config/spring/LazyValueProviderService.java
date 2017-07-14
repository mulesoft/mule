/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.value.ResolvingFailure.Builder.newFailure;
import static org.mule.runtime.api.value.ValueResult.resultFrom;
import static org.mule.runtime.core.internal.value.MuleValueProviderServiceUtility.deleteLastPartFromLocation;
import static org.mule.runtime.core.internal.value.MuleValueProviderServiceUtility.isConnection;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.core.internal.value.MuleValueProviderService;

import java.util.Optional;

/**
 * {@link ValueProviderService} implementation flavour that initialises just the required components before executing
 * the resolving logic.
 * <p>
 * This guarantees that if the application has been created lazily, the requested components exists before the execution
 * of the actual {@link ValueProviderService}.
 *
 * @since 4.0
 * @see ValueProviderService
 */
public class LazyValueProviderService implements ValueProviderService {

  private LazyMuleArtifactContext lazyMuleArtifactContext;
  private ValueProviderService providerService;

  LazyValueProviderService(LazyMuleArtifactContext artifactContext, MuleValueProviderService providerServiceDelegate) {
    this.lazyMuleArtifactContext = artifactContext;
    this.providerService = providerServiceDelegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ValueResult getValues(Location location, String providerName) {
    return initializeComponent(locationWithOutConnection(location))
        .orElseGet(() -> providerService.getValues(location, providerName));
  }

  private Optional<ValueResult> initializeComponent(Location location) {
    try {
      lazyMuleArtifactContext.initializeComponent(location);
    } catch (Exception e) {
      return of(resultFrom(newFailure(e).build()));
    }
    return empty();
  }

  private Location locationWithOutConnection(Location location) {
    return isConnection(location) ? deleteLastPartFromLocation(location) : location;
  }
}
