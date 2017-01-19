/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import org.mule.extension.oauth2.internal.TokenRequestHandler;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Handler for calling the token url, parsing the response and storing the oauth context data.
 */
public class ClientCredentialsTokenRequestHandler extends TokenRequestHandler {

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean encodeClientCredentialsInBody;

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  public String getScopes() {
    return scopes;
  }

  public void setEncodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }

  public boolean isEncodeClientCredentialsInBody() {
    return encodeClientCredentialsInBody;
  }
}
