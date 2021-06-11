/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class OAuthValuesProvider implements ValueProvider {

  @org.mule.sdk.api.annotation.param.Connection
  private TestOAuthConnection testOAuthConnection;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    if (!testOAuthConnection.getState().getState().getAccessToken().contains("refresh")) {
      throw new AccessTokenExpiredException();
    }
    return getValuesFor("Option1", "Option2", "Option3");
  }
}
