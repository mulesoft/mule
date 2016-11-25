/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.jndi;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.DelegatingConnectionFactory;

/**
 * A {@link ConnectionFactory} that wraps a {@link ConnectionFactory delegate}
 * that is discovered using a {@link JndiNameResolver}
 *
 * @since 4.0
 */
public final class JndiConnectionFactory extends DelegatingConnectionFactory implements Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(JndiConnectionFactory.class);

  /**
   * Name of the ConnectionFactory to be discovered using Jndi
   * and used as a delegate of {@code this} {@link ConnectionFactory}
   */
  @Parameter
  private String connectionFactoryJndiName;

  /**
   * The {@link LookupJndiDestination} policy to use when creating {@link Destination}s
   */
  @Parameter
  @Optional(defaultValue = "NEVER")
  private LookupJndiDestination lookupDestination;

  /**
   *  Provider for the {@link JndiNameResolver}
   */
  @ParameterGroup("Jndi Name Resolver")
  private JndiNameResolverProvider nameResolverProvider;


  private JndiNameResolver nameResolver;


  public java.util.Optional<Destination> getJndiDestination(String name) {

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Looking up %s from JNDI", name));
      }

      Object temp = lookupFromJndi(name);

      return temp instanceof Destination ? of((Destination) temp) : empty();

    } catch (NamingException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Failed to look up destination [%s]: ", name), e);
      }

      return empty();
    }
  }


  @Override
  public void initialise() throws InitialisationException {

    try {
      setupNameResolver();

      Object temp = getJndiNameResolver().lookup(connectionFactoryJndiName);
      if (temp instanceof ConnectionFactory) {
        this.setTargetConnectionFactory((ConnectionFactory) temp);
      } else {
        throw new IllegalArgumentException("No valid ConnectionFactory was provided. Unable to initialise.");
      }
    } catch (NamingException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Failed to initialise the Connection factory: ", e);
      }
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(getJndiNameResolver());
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(getJndiNameResolver());
  }

  @Override
  public void dispose() {
    disposeIfNeeded(getJndiNameResolver(), LOGGER);
  }

  private void setupNameResolver() throws InitialisationException {
    JndiNameResolver customJndiNameResolver = nameResolverProvider.getCustomJndiNameResolver();
    if (customJndiNameResolver != null) {
      nameResolver = customJndiNameResolver;
    } else {
      nameResolver = nameResolverProvider.createDefaultJndiResolver();
    }

    initialiseIfNeeded(nameResolver);
  }

  private Object lookupFromJndi(String jndiName) throws NamingException {
    try {
      return getJndiNameResolver().lookup(jndiName);
    } catch (NamingException e) {
      //TODO MULE-10959: mark transaction for rollback
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Failed to resolve lookup for name [%s]", jndiName), e);
      }
      throw e;
    }
  }

  private JndiNameResolver getJndiNameResolver() {
    return nameResolver;
  }

  @Override
  public JMSContext createContext() {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(String userName, String password) {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(String userName, String password, int sessionMode) {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(int sessionMode) {
    // We'll use the classic API
    return null;
  }

  public String getConnectionFactoryJndiName() {
    return connectionFactoryJndiName;
  }

  public LookupJndiDestination getLookupDestination() {
    return lookupDestination;
  }

  public JndiNameResolverProvider getNameResolverProvider() {
    return nameResolverProvider;
  }

}
