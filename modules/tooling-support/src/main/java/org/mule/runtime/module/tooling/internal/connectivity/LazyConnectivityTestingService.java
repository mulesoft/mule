/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.connectivity;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;

import static com.google.common.base.Throwables.getCausalChain;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.exception.ObjectNotFoundException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.internal.context.lazy.NoSuchComponentModelException;

import java.util.List;
import java.util.function.Function;

import jakarta.inject.Inject;

/**
 * {@link ConnectivityTestingService} implementation that initialises the required components before doing test connectivity.
 *
 * This guarantees that if the application has been created lazily, the requested components exists before the execution of the
 * actual {@link ConnectivityTestingService}.
 */
public class LazyConnectivityTestingService implements ConnectivityTestingService, Initialisable {

  public static final String NON_LAZY_CONNECTIVITY_TESTING_SERVICE = "_muleNonLazyConnectivityTestingService";

  private final Function<Registry, ConnectivityTestingService> connectivityTestingServiceSupplier;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Inject
  private Registry registry;

  private ConnectivityTestingService connectivityTestingService;

  public LazyConnectivityTestingService(Function<Registry, ConnectivityTestingService> connectivityTestingServiceSupplier) {
    this.connectivityTestingServiceSupplier = connectivityTestingServiceSupplier;
  }

  @Override
  public ConnectionValidationResult testConnection(Location location) {
    try {
      lazyComponentInitializer.initializeComponent(location);
    } catch (NoSuchComponentModelException e) {
      throw new ObjectNotFoundException(location.toString());
    } catch (MuleRuntimeException e) {
      List<Throwable> causalChain = getCausalChain(e);
      return unknownFailureResponse(lastMessage(causalChain), e);
    } catch (Exception e) {
      return unknownFailureResponse(e.getCause().getMessage(), e);
    }
    return connectivityTestingService.testConnection(location);
  }

  private ConnectionValidationResult unknownFailureResponse(String message, Exception e) {
    return failure(message, e);
  }

  private String lastMessage(List<Throwable> causalChain) {
    return causalChain.get(causalChain.size() - 1).getMessage();
  }

  @Override
  public void initialise() throws InitialisationException {
    this.connectivityTestingService = connectivityTestingServiceSupplier.apply(registry);
  }
}
