/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static com.google.common.base.Throwables.getCausalChain;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.spring.dsl.model.NoSuchComponentModelException;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.exception.ObjectNotFoundException;

/**
 * {@link ConnectivityTestingService} implementation that initialises the required
 * components before doing test connectivity.
 *
 * This guarantees that if the application has been created lazily, the requested
 * components exists before the execution of the actual {@link ConnectivityTestingService}.
 */
public class LazyConnectivityTestingService implements ConnectivityTestingService {

  private final LazyComponentInitializer lazyComponentInitializer;
  private final ConnectivityTestingService connectivityTestingService;

  public LazyConnectivityTestingService(LazyComponentInitializer lazyComponentInitializer,
                                        ConnectivityTestingService connectivityTestingService) {
    this.lazyComponentInitializer = lazyComponentInitializer;
    this.connectivityTestingService = connectivityTestingService;
  }

  @Override
  public ConnectionValidationResult testConnection(Location location) {
    try {
      lazyComponentInitializer.initializeComponent(location);
    } catch (MuleRuntimeException e) {
      if (e.getCause() instanceof NoSuchComponentModelException) {
        throw new ObjectNotFoundException(location.toString());
      }
      return getCausalChain(e).stream()
          .filter(exception -> exception.getClass().equals(ConnectionException.class)
              && ((ConnectionException) exception).getErrorType().isPresent())
          .map(exception -> failure(exception.getMessage(), ((ConnectionException) exception).getErrorType().get(),
                                    (Exception) exception))
          .findFirst()
          .orElse(unknownFailureResponse(e));
    } catch (Exception e) {
      return unknownFailureResponse(e);
    }
    return connectivityTestingService.testConnection(location);
  }

  private ConnectionValidationResult unknownFailureResponse(Exception e) {
    return failure(e.getMessage(), e);
  }
}
