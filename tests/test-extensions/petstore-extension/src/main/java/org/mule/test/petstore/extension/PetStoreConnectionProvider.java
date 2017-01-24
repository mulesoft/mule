/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public abstract class PetStoreConnectionProvider<T extends PetStoreClient> implements ConnectionProvider<T>, Lifecycle {

  @Inject
  protected MuleContext muleContext;
  @ConfigName
  protected String configName;
  @Parameter
  protected String username;
  @Parameter
  @Password
  protected String password;
  @Parameter
  @Optional
  protected TlsContextFactory tls;
  @Optional
  @Parameter
  protected Date openingDate;
  @Optional
  @Parameter
  protected List<Date> closedForHolidays;
  @Optional
  @Parameter
  protected List<LocalDateTime> discountDates;
  private int initialise, start, stop, dispose = 0;

  @Override
  public T connect() {
    return (T) new PetStoreClient(username, password, tls, configName, openingDate, closedForHolidays, discountDates);
  }

  @Override
  public void disconnect(PetStoreClient connection) {
    if (connection != null) {
      connection.disconnect();
    }
  }

  @Override
  public ConnectionValidationResult validate(PetStoreClient connection) {
    if (connection.getUsername().equals("john") && connection.getPassword().equals("doe")) {
      return ConnectionValidationResult.success();
    } else {
      return ConnectionValidationResult.failure("Invalid credentials", new Exception("Invalid credentials"));
    }
  }

  @Override
  public void dispose() {
    dispose++;
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

  public MuleContext getMuleContext() {
    return muleContext;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Date getOpeningDate() {
    return openingDate;
  }
}
