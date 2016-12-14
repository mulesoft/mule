/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.connection.provider;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_1_0_2b;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.runtime.api.connection.ConnectionExceptionCode.DISCONNECTED;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.connection.caching.CachingStrategy;
import org.mule.extensions.jms.api.connection.caching.DefaultCachingStrategy;
import org.mule.extensions.jms.api.exception.JmsCallbackConnectionException;
import org.mule.extensions.jms.internal.connection.JmsCachingConnectionFactory;
import org.mule.extensions.jms.internal.connection.param.GenericConnectionParameters;
import org.mule.extensions.jms.internal.support.Jms102bSupport;
import org.mule.extensions.jms.internal.support.Jms11Support;
import org.mule.extensions.jms.internal.support.Jms20Support;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;

import org.slf4j.Logger;

/**
 * Base implementation of a {@link PoolingConnectionProvider} for {@link JmsConnection}s
 *
 * @since 4.0
 */
public abstract class BaseConnectionProvider implements PoolingConnectionProvider<JmsConnection>, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(BaseConnectionProvider.class);

  @ParameterGroup(CONNECTION)
  private GenericConnectionParameters connectionParameters;

  /**
   * the strategy to be used for caching of {@link Session}s and {@link Connection}s
   */
  @Parameter
  @Optional
  @NullSafe(defaultImplementingType = DefaultCachingStrategy.class)
  private CachingStrategy cachingStrategy;


  /**
   * Used to ignore handling of ExceptionListener#onException when in the process of disconnecting
   */
  private AtomicBoolean disconnecting = new AtomicBoolean(false);

  private JmsSupport jmsSupport;
  private ConnectionFactory jmsConnectionFactory;
  private boolean isCacheEnabled = false;

  /**
   * Template method for obtaining the {@link ConnectionFactory} to be used for creating the {@link JmsConnection}s
   * @return an instance of {@link ConnectionFactory} to be used for creating the {@link JmsConnection}s
   * @throws Exception if an error occurs while creting the {@link ConnectionFactory}
   */
  public abstract ConnectionFactory getConnectionFactory() throws Exception;

  @Override
  public void initialise() throws InitialisationException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Executing initialise for [%s]", getClass().getName()));
    }
    try {
      createJmsSupport();
      initialiseConnectionFactory();

    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Failed to initialise [%s]: ", getClass().getName()), e);
      }
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public JmsConnection connect() throws ConnectionException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Connection Started");
    }

    disconnecting.set(false);
    try {
      Connection connection = createConnection();
      connection.start();
      return new JmsConnection(jmsSupport, connection);

    } catch (Exception e) {
      try {
        // If connection throws an exception on start and connection is cached in ConnectionFactory then
        // stop/reset connection now.
        stopIfNeeded(jmsConnectionFactory);

      } catch (MuleException factoryStopException) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Failed to reset cached connection: ", factoryStopException);
        }
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Failed create connection: ", e);
      }

      throw new ConnectionException(e);
    }
  }

  @Override
  public ConnectionValidationResult validate(JmsConnection jmsConnection) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Validating connection");
    }

    try {
      // According to javax.jms.Connection#start javadoc:
      // 'a call to start on a connection that has already been started is ignored'
      // and exception is thrown 'if the JMS provider fails to start'
      // thus, if the connection is valid, we should be able to re-start it even if the 'connect'
      // method did it already
      jmsConnection.get().start();
      return success();
    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Validation failed: ", e);
      }
      return failure("Invalid connection provided: Connection could not be started.", DISCONNECTED, e);
    }
  }

  @Override
  public void disconnect(JmsConnection jmsConnection) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Disconnection Started");
    }
    // This invocations will be ignored when using a CachingConnectionFactory,
    // since a single connection is cached
    disconnecting.set(true);
    doStop(jmsConnection);
    doClose(jmsConnection);
  }

  @Override
  public void onReturn(JmsConnection connection) {
    connection.releaseResources();
  }

  protected void doStop(JmsConnection jmsConnection) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Perform doStop: [%s]", getClass().getName()));
    }

    try {
      stopIfNeeded(jmsConnection);
    } catch (Exception e) {
      // this exception may be thrown when the broker is shut down, but the
      // stop process should continue all the same
      LOGGER.warn("Jms connection failed to stop properly: ", e);
    }
  }

  protected void doClose(JmsConnection jmsConnection) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Perform doClose: [%s]", getClass().getName()));
    }
    disposeIfNeeded(jmsConnection, LOGGER);
  }

  @Override
  public void dispose() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Disposing [%s]", getClass().getName()));
    }
    disposeIfNeeded(jmsConnectionFactory, LOGGER);
  }

  private void initialiseConnectionFactory() throws Exception {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Initialising Connection Factory");
    }

    ConnectionFactory targetFactory = getConnectionFactory();

    initialiseIfNeeded(targetFactory);

    if (cachingStrategy.appliesTo(targetFactory) && cachingStrategy.strategyConfiguration().isPresent()) {
      isCacheEnabled = true;

      String username = getConnectionParameters().getUsername();
      String password = getConnectionParameters().getPassword();
      String clientId = getConnectionParameters().getClientId();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Using CachingConnectionFactory wrapper with: username:[%s], password:[%s], clientId:[%s]",
                            username, password, clientId));
      }
      jmsConnectionFactory =
          new JmsCachingConnectionFactory(targetFactory, username, password, clientId,
                                          cachingStrategy.strategyConfiguration().get(),
                                          jmsSupport, getExceptionListener());

      initialiseIfNeeded(jmsConnectionFactory);
    } else {

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Skip CachingConnectionFactory Wrapper"));
      }

      jmsConnectionFactory = targetFactory;
    }
  }

  /**
   * A jmsConnectionFactory method to create various JmsSupport class versions.
   *
   * @see JmsSupport
   */
  protected void createJmsSupport() {
    JmsSpecification specification = getConnectionParameters().getSpecification();
    if (JMS_1_0_2b.equals(specification)) {
      jmsSupport = new Jms102bSupport();

    } else if (JMS_2_0.equals(specification)) {
      jmsSupport = new Jms20Support();

    } else {
      jmsSupport = new Jms11Support();
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("JMS Support set to [%s]", jmsSupport.getSpecification().getName()));
    }
  }

  private Connection createConnection() throws JMSException {

    String username = getConnectionParameters().getUsername();
    String password = getConnectionParameters().getPassword();

    Connection connection;
    if (isCacheEnabled || (isBlank(username))) {
      connection = jmsSupport.createConnection(jmsConnectionFactory);
    } else {
      connection = jmsSupport.createConnection(jmsConnectionFactory, username, password);
    }

    if (connection == null) {
      throw new IllegalStateException("An error occurred, Connection cannot be null after creation");
    }

    if (!isCacheEnabled) {
      String clientId = getConnectionParameters().getClientId();
      if (!isBlank(clientId) && !clientId.equals(connection.getClientID())) {
        connection.setClientID(clientId);
      }

      if (connection.getExceptionListener() == null) {
        try {
          connection.setExceptionListener(getExceptionListener());
        } catch (Exception e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("An error occurred while setting the ExceptionListener. "
                + "No ExceptionListener is available in a Java EE web or EJB application. ", e);
          }
        }
      }
    }

    return connection;
  }

  private ExceptionListener getExceptionListener() {
    return e -> {
      if (!disconnecting.get()) {
        throw new JmsCallbackConnectionException(e);
      }
    };
  }

  public GenericConnectionParameters getConnectionParameters() {
    return connectionParameters;
  }

  public JmsSupport getJmsSupport() {
    return jmsSupport;
  }

  protected void setJmsSupport(JmsSupport jmsSupport) {
    this.jmsSupport = jmsSupport;
  }

}
