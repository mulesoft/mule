/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.complex.config.properties.deprecated.extension.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;

import java.util.List;
import java.util.Map;

@Alias("provider")
public class ComplexTypesConnectionProvider implements ConnectionProvider<PropertiesRepositoryConnection>, NoConnectivityTest {

  @Parameter
  private SomePojo nestedPojo;

  @Parameter
  private List<String> listedTexts;

  @Parameter
  private List<SomePojo> listedPojos;

  @Parameter
  private Map<String, SomePojo> mappedPojos;

  @Override
  public PropertiesRepositoryConnection connect() throws ConnectionException {
    return new PropertiesRepositoryConnection();
  }

  @Override
  public void disconnect(PropertiesRepositoryConnection connection) {

  }

  @Override
  public ConnectionValidationResult validate(PropertiesRepositoryConnection connection) {
    return success();
  }

}
