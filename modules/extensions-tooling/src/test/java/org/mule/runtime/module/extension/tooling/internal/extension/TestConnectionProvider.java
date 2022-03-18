/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.extension;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.module.extension.tooling.internal.TestToolingExtensionDeclarer.DEFAULT_HOST;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import javax.inject.Inject;

public class TestConnectionProvider implements ConnectionProvider<Connection>, Lifecycle {

  public static final String COMPLEX_PARAMETER_GROUP_NAME = "COMPLEX";

  private Connection connection = new Connection();
  private int initialise, start, stop, dispose = 0;

  @Inject
  private LockFactory lockFactory;

  @RefName
  private String configName;

  @DefaultEncoding
  private String encoding;

  @Inject
  private SchedulerService schedulerService;

  @Parameter
  private String username;

  @Parameter
  @Password
  private String password;

  @Parameter
  @Optional(defaultValue = DEFAULT_HOST)
  private String host;

  @Parameter
  private Integer port;

  @ParameterGroup(name = COMPLEX_PARAMETER_GROUP_NAME)
  private ComplexParameterGroup complexParameterGroup;

  @Override
  public Connection connect() throws ConnectionException {
    return connection;
  }

  @Override
  public void disconnect(Connection connection) {
    connection.setConnected(false);
  }

  @Override
  public ConnectionValidationResult validate(Connection connection) {
    if (connection.isConnected()) {
      return success();
    } else {
      return failure("Not connected", new IllegalStateException());
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialise++;
  }

  @Override
  public void start() throws MuleException {
    start++;
  }

  @Override
  public void stop() throws MuleException {
    stop++;
  }

  @Override
  public void dispose() {
    dispose++;
  }

  public Connection getConnection() {
    return connection;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public int getInitialise() {
    return initialise;
  }

  public int getStart() {
    return start;
  }

  public int getStop() {
    return stop;
  }

  public int getDispose() {
    return dispose;
  }

  public LockFactory getLockFactory() {
    return lockFactory;
  }

  public String getConfigName() {
    return configName;
  }

  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  public String getEncoding() {
    return encoding;
  }

  public ComplexParameterGroup getComplexParameterGroup() {
    return complexParameterGroup;
  }
}
