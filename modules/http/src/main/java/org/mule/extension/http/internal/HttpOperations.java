/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import static org.mule.extension.http.api.error.HttpError.SECURITY;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.error.ResourceNotFoundException;
import org.mule.extension.http.api.listener.HttpBasicAuthenticationFilter;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;

import javax.inject.Inject;

/**
 * General HTTP operations that do not required any specific configuration or connection.
 *
 * @since 4.0
 */
public class HttpOperations {

  @Inject
  private MuleContext muleContext;

  /**
   * Authenticates received HTTP requests. Must be used after a listener component.
   *
   * @param realm Authentication realm.
   * @param securityProviders The delegate-security-provider to use for authenticating. Use this in case you have multiple
   *        security managers defined in your configuration.
   * @throws MuleException if unauthenticated.
   */
  @Throws(BasicSecurityErrorTypeProvider.class)
  public void basicSecurityFilter(String realm, @Optional String securityProviders,
                                  @Optional(defaultValue = "#[attributes]") HttpRequestAttributes attributes, Event event)
      throws MuleException {
    HttpBasicAuthenticationFilter filter = createFilter(realm, securityProviders, attributes);

    try {
      filter.doFilter(event);
    } catch (Exception e) {
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

  private HttpBasicAuthenticationFilter createFilter(String realm, String securityProviders, HttpRequestAttributes attributes)
      throws InitialisationException {
    HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter();
    filter.setRealm(realm);
    filter.setSecurityProviders(securityProviders);
    filter.setMuleContext(muleContext);
    filter.setAttributes(attributes);
    filter.initialise();
    return filter;
  }
}
