/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.api.meta.LibraryType.NATIVE;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.atomic.AtomicInteger;

@ExternalLib(name = HeisenbergExtension.HEISENBERG_LIB_NAME, description = HeisenbergExtension.HEISENBERG_LIB_DESCRIPTION,
    nameRegexpMatcher = HeisenbergExtension.HEISENBERG_LIB_FILE_NAME,
    requiredClassName = HeisenbergExtension.HEISENBERG_LIB_CLASS_NAME, type = NATIVE)
public class HeisenbergConnectionProvider implements ConnectionProvider<HeisenbergConnection> {

  public static final String SAUL_OFFICE_NUMBER = "505-503-4455";

  private static final AtomicInteger connects = new AtomicInteger();
  private static final AtomicInteger disconnects = new AtomicInteger();

  @Parameter
  @Optional(defaultValue = SAUL_OFFICE_NUMBER)
  private String saulPhoneNumber;

  @Parameter
  @Optional
  private TlsContextFactory tlsContextFactory;

  @Override
  public HeisenbergConnection connect() throws ConnectionException {
    connects.incrementAndGet();
    return new HeisenbergConnection(saulPhoneNumber);
  }

  @Override
  public void disconnect(HeisenbergConnection heisenbergConnection) {
    disconnects.incrementAndGet();
  }

  @Override
  public ConnectionValidationResult validate(HeisenbergConnection heisenbergConnection) {
    return ConnectionValidationResult.success();
  }

  public static int getConnects() {
    return connects.get();
  }

  public static int getDisconnects() {
    return disconnects.get();
  }
}
