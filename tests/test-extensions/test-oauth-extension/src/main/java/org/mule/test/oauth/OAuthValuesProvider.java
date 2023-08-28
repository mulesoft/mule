/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.sdk.api.connectivity.oauth.AccessTokenExpiredException;

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
