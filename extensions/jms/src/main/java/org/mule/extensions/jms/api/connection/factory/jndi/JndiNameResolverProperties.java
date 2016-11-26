/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.jndi;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Map;

import javax.naming.InitialContext;

/**
 * Declares the properties required to create a {@link JndiNameResolver}
 *
 * @since 4.0
 */
public final class JndiNameResolverProperties {

  /**
   * The fully qualified class name of the factory class that will create an {@link InitialContext}
   */
  @Parameter
  @Summary("The fully qualified class name of the factory class that will create an initial context")
  private String jndiInitialContextFactory;

  /**
   * The JNDI service provider URL
   */
  @Parameter
  @Optional
  @Summary("The JNDI service provider URL")
  private String jndiProviderUrl;

  /**
   * Properties to be passed on to the JNDI Name Resolver Context
   */
  @Parameter
  @Optional
  @Summary("Properties to be passed on to the JNDI Name Resolver Context")
  private Map<String, Object> providerProperties;

  public String getJndiInitialContextFactory() {
    return jndiInitialContextFactory;
  }

  public Map<String, Object> getProviderProperties() {
    return providerProperties;
  }

  public String getJndiProviderUrl() {
    return jndiProviderUrl;
  }

}
