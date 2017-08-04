/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import static com.google.common.base.Throwables.getCausalChain;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.connectivity.UnsupportedConnectivityTestingObjectException;
import org.mule.runtime.api.exception.ObjectNotFoundException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.spring.internal.dsl.model.NoSuchComponentModelException;

import java.util.List;
import java.util.function.Supplier;

/**
 * {@link ConnectivityTestingService} implementation that initialises the required components before doing test connectivity.
 *
 * This guarantees that if the application has been created lazily, the requested components exists before the execution of the
 * actual {@link ConnectivityTestingService}.
 */
public class LazyConnectivityTestingService implements ConnectivityTestingService, Initialisable {

  public static final String NON_LAZY_CONNECTIVITY_TESTING_SERVICE = "_muleNonLazyConnectivityTestingService";

  private final LazyComponentTaskExecutor lazyComponentTaskExecutor;
  private final Supplier<ConnectivityTestingService> connectivityTestingServiceSupplier;

  private ConnectivityTestingService connectivityTestingService;

  public LazyConnectivityTestingService(LazyComponentTaskExecutor lazyComponentTaskExecutor,
                                        Supplier<ConnectivityTestingService> connectivityTestingServiceSupplier) {
    this.lazyComponentTaskExecutor = lazyComponentTaskExecutor;
    this.connectivityTestingServiceSupplier = connectivityTestingServiceSupplier;
  }

  @Override
  public ConnectionValidationResult testConnection(Location location) {
    try {
      return lazyComponentTaskExecutor
          .withContext(location, () -> connectivityTestingService.testConnection(location));
    } catch (UnsupportedConnectivityTestingObjectException e) {
      throw e;
    } catch (NoSuchComponentModelException e) {
      throw new ObjectNotFoundException(location.toString());
    } catch (Exception e) {
      List<Throwable> causalChain = getCausalChain(e);
      return causalChain.stream()
          .filter(exception -> exception.getClass().equals(ConnectionException.class)
              && ((ConnectionException) exception).getErrorType().isPresent())
          .map(exception -> failure(exception.getMessage(),
                                    ((ConnectionException) exception).getErrorType().get(),
                                    (Exception) exception))
          .findFirst()
          .orElse(unknownFailureResponse(lastMessage(causalChain), e));
    }
  }

  private ConnectionValidationResult unknownFailureResponse(String message, Exception e) {
    return failure(message, e);
  }

  private String lastMessage(List<Throwable> causalChain) {
    return causalChain.get(causalChain.size() - 1).getMessage();
  }

  @Override
  public void initialise() throws InitialisationException {
    this.connectivityTestingService = connectivityTestingServiceSupplier.get();
  }
}
