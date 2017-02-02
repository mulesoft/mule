/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.jndi;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.slf4j.Logger;

/**
 * Base class for implementing a custom {@link JndiNameResolver}
 *
 * @since 4.0
 */
abstract class AbstractJndiNameResolver implements JndiNameResolver {

  protected final Logger LOGGER = getLogger(getClass());

  private String jndiProviderUrl;
  private String jndiInitialFactory;
  private Map<String, Object> jndiProviderProperties;

  // Default JNDI InitialContextFactory
  private InitialContextFactory contextFactory = InitialContext::new;

  /**
   * Creates a JNDI context using the current {@link #contextFactory}
   *
   * @return a new {@link Context} instance. Callers must provide concurrent
   * access control on the returned value.
   * @throws NamingException if there is a problem during the context creation.
   */
  protected Context createInitialContext() throws NamingException {
    return contextFactory.getInitialContext(getContextProperties());
  }

  protected Hashtable getContextProperties() {
    checkArgument(jndiInitialFactory != null, "Undefined value for jndiInitialFactory property");

    Hashtable<String, Object> props = new Hashtable<String, Object>();
    props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);

    if (jndiProviderUrl != null) {
      props.put(Context.PROVIDER_URL, jndiProviderUrl);
    }

    if (jndiProviderProperties != null) {
      props.putAll(jndiProviderProperties);
    }

    return props;
  }

  public String getJndiProviderUrl() {
    return jndiProviderUrl;
  }

  public void setJndiProviderUrl(String jndiProviderUrl) {
    this.jndiProviderUrl = jndiProviderUrl;
  }

  public String getJndiInitialFactory() {
    return jndiInitialFactory;
  }

  public void setJndiInitialFactory(String jndiInitialFactory) {
    this.jndiInitialFactory = jndiInitialFactory;
  }

  public Map<String, Object> getJndiProviderProperties() {
    return jndiProviderProperties;
  }

  public void setJndiProviderProperties(Map<String, Object> jndiProviderProperties) {
    this.jndiProviderProperties = jndiProviderProperties;
  }

  public InitialContextFactory getContextFactory() {
    return contextFactory;
  }

  public void setContextFactory(InitialContextFactory contextFactory) {
    if (contextFactory == null) {
      throw new IllegalArgumentException("Context factory cannot be null");
    }

    this.contextFactory = contextFactory;
  }

  @Override
  public void initialise() throws InitialisationException {
    // Does nothing
  }

  @Override
  public void dispose() {
    // Does nothing
  }

  @Override
  public void start() throws MuleException {
    // Does nothing
  }

  @Override
  public void stop() throws MuleException {
    // Does nothing
  }
}
