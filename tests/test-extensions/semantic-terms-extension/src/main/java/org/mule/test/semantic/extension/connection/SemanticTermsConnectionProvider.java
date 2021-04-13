/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.Domain;
import org.mule.sdk.api.annotation.semantics.connectivity.Endpoint;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.connectivity.Url;
import org.mule.sdk.api.annotation.semantics.connectivity.UrlPath;
import org.mule.test.semantic.extension.ProxyConfiguration;

public abstract class SemanticTermsConnectionProvider implements ConnectionProvider<SemanticConnection> {

  @Parameter
  @Host
  private String host;

  @Parameter
  @Port
  private int port;

  @Parameter
  @Url
  private String url;

  @Parameter
  @Domain
  private String domain;

  @Parameter
  @UrlPath
  private String urlPath;

  @Parameter
  private ProxyConfiguration proxyConfiguration;

  @Parameter
  @Endpoint
  private String endpoint;

  @Override
  public SemanticConnection connect() throws ConnectionException {
    return new SemanticConnection();
  }

  @Override
  public void disconnect(SemanticConnection semanticConnection) {

  }

  @Override
  public ConnectionValidationResult validate(SemanticConnection semanticConnection) {
    return success();
  }
}
