/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.extension.file.common.api.FileSystemProvider;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Base class for {@link ConnectionProvider} implementations which take a {@link FtpConnector} as a config and provides a
 * {@link FtpFileSystem}
 *
 * @param <C> the generic type of the connection object
 * @since 4.0
 */
public abstract class AbstractFtpConnectionProvider<C extends FtpFileSystem>
    extends FileSystemProvider<C> implements PoolingConnectionProvider<C> {

  private static final String TIMEOUT_CONFIGURATION = "Timeout Configuration";

  @Inject
  protected MuleContext muleContext;

  /**
   * The directory to be considered as the root of every relative path used with this connector. If not provided, it will default
   * to the remote server default.
   */
  @Parameter
  @Optional
  @Summary("The directory to be considered as the root of every relative path used with this connector")
  @DisplayName("Working Directory")
  private String workingDir = null;

  @ParameterGroup(name = TIMEOUT_CONFIGURATION)
  private TimeoutSettings timeoutSettings = new TimeoutSettings();

  /**
   * Invokes the {@link ClassicFtpFileSystem#disconnect()} method on the given {@code ftpFileSystem}
   *
   * @param ftpFileSystem a {@link ClassicFtpFileSystem} instance
   */
  @Override
  public void disconnect(C ftpFileSystem) {
    ftpFileSystem.disconnect();
  }

  /**
   * Validates the connection by delegating into {@link FtpFileSystem#validateConnection()}
   *
   * @param ftpFileSystem the connection to validate
   * @return a {@link ConnectionValidationResult}
   */
  @Override
  public ConnectionValidationResult validate(C ftpFileSystem) {
    return ftpFileSystem.validateConnection();
  }

  /**
   * {@inheritDoc}
   */
  public String getWorkingDir() {
    return workingDir;
  }

  protected Integer getConnectionTimeout() {
    return timeoutSettings.getConnectionTimeout();
  }

  protected TimeUnit getConnectionTimeoutUnit() {
    return timeoutSettings.getConnectionTimeoutUnit();
  }

  protected Integer getResponseTimeout() {
    return timeoutSettings.getResponseTimeout();
  }

  protected TimeUnit getResponseTimeoutUnit() {
    return timeoutSettings.getResponseTimeoutUnit();
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    timeoutSettings.setConnectionTimeout(connectionTimeout);
  }

  public void setConnectionTimeoutUnit(TimeUnit connectionTimeoutUnit) {
    timeoutSettings.setConnectionTimeoutUnit(connectionTimeoutUnit);
  }

  public void setResponseTimeout(Integer responseTimeout) {
    timeoutSettings.setResponseTimeout(responseTimeout);
  }

  public void setResponseTimeoutUnit(TimeUnit responseTimeoutUnit) {
    timeoutSettings.setResponseTimeoutUnit(responseTimeoutUnit);
  }
}
