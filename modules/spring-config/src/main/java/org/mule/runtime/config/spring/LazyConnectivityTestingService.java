/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.api.connection.ConnectionExceptionCode.UNKNOWN;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
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
  public ConnectionValidationResult testConnection(String identifier) {
    try {
      lazyComponentInitializer.initializeComponent(identifier);
    } catch (MuleRuntimeException e) {
      if (e.getCause() instanceof NoSuchComponentModelException) {
        throw new ObjectNotFoundException(identifier);
      }
      return unknownFailureResponse(e);
    } catch (Exception e) {
      return unknownFailureResponse(e);
    }
    return connectivityTestingService.testConnection(identifier);
  }

  private ConnectionValidationResult unknownFailureResponse(Exception e) {
    return failure(e.getMessage(), UNKNOWN, e);
  }
}
