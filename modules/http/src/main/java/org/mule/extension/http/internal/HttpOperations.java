/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import static org.mule.extension.http.api.error.HttpError.FORBIDDEN;
import static org.mule.extension.http.api.error.HttpError.SECURITY;
import static org.mule.extension.http.api.error.HttpError.UNAUTHORIZED;
import org.mule.extension.http.api.error.ResourceNotFoundException;
import org.mule.extension.http.api.listener.HttpBasicAuthenticationFilter;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.security.AuthenticationHandler;

/**
 * General HTTP operations that do not required any specific configuration or connection.
 *
 * @since 4.0
 */
public class HttpOperations {

  /**
   * Authenticates received HTTP requests. Must be used after a listener component.
   */
  @Throws(BasicSecurityErrorTypeProvider.class)
  public void basicSecurityFilter(@ParameterGroup(name = "Security Filter") HttpBasicAuthenticationFilter filter,
                                  AuthenticationHandler authenticationHandler) {
    try {
      filter.authenticate(authenticationHandler);
    } catch (UnauthorisedException e) {
      throw new ModuleException(e, UNAUTHORIZED);
    } catch (SecurityException e) {
      throw new ModuleException(e, FORBIDDEN);
    } catch (SecurityProviderNotFoundException | UnknownAuthenticationTypeException e) {
      throw new ModuleException(e, SECURITY);
    }
  }

  /**
   * Serves up static content for use with HTTP, using the request path to lookup the resource.
   *
   * @return the resource defined by the path of an HTTP request
   */
  @Throws(LoadStaticResourceErrorTypeProvider.class)
  public Result<?, ?> loadStaticResource(@ParameterGroup(name = "Resource") StaticResourceLoader resourceLoader)
      throws ResourceNotFoundException {
    return resourceLoader.load();
  }

}
