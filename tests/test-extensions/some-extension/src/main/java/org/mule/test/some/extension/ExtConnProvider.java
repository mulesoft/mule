/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.some.extension;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.OAUTH2;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.ModuleException;

public class ExtConnProvider implements CachedConnectionProvider<String> {

  private static final ModuleException OAUTH_MODULE_EXCEPTION = new ModuleException(OAUTH2, new RuntimeException());
  private static final ModuleException HEALTH_MODULE_EXCEPTION = new ModuleException(HEALTH, new RuntimeException());
  private static final CustomConnectionException DOMAIN_HEALTH_CONNECTION_EXCEPTION =
      new CustomConnectionException(HEALTH_MODULE_EXCEPTION);
  private static final CustomConnectionException DOMAIN_OAUTH_CONNECTION_EXCEPTION =
      new CustomConnectionException(OAUTH_MODULE_EXCEPTION);
  private static final ConnectionException CONNECTION_EXCEPTION = new ConnectionException("Some Error", HEALTH_MODULE_EXCEPTION);

  @Parameter
  @Optional(defaultValue = "false")
  public boolean fail;

  @Parameter
  @Optional(defaultValue = "false")
  public boolean domainException;

  @Override
  public String connect() throws ConnectionException {
    if (fail) {
      throw domainException
          ? DOMAIN_HEALTH_CONNECTION_EXCEPTION
          : CONNECTION_EXCEPTION;
    }
    return "";
  }

  @Override
  public void disconnect(String connection) {

  }

  @Override
  public ConnectionValidationResult validate(String connection) {
    return domainException
        ? failure("This is a failure", DOMAIN_OAUTH_CONNECTION_EXCEPTION)
        : failure("This is a failure", OAUTH_MODULE_EXCEPTION);
  }


}
