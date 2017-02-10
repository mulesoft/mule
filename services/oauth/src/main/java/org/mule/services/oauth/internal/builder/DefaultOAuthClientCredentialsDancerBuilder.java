/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal.builder;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.oauth.api.OAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpService;
import org.mule.services.oauth.internal.ClientCredentialsOAuthDancer;

import java.util.Map;


public class DefaultOAuthClientCredentialsDancerBuilder extends AbstractOAuthDancerBuilder
    implements OAuthClientCredentialsDancerBuilder {

  private boolean encodeClientCredentialsInBody = false;

  public DefaultOAuthClientCredentialsDancerBuilder(LockFactory lockProvider,
                                                    Map<String, ResourceOwnerOAuthContext> tokensStore, HttpService httpService,
                                                    ExpressionEvaluator expressionEvaluator) {
    super(lockProvider, tokensStore, httpService, expressionEvaluator);
  }

  @Override
  public OAuthClientCredentialsDancerBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
    return this;
  }

  @Override
  public OAuthDancer build() {
    Preconditions.checkArgument(isNotBlank(clientId), "clientId cannot be blank");
    Preconditions.checkArgument(isNotBlank(clientSecret), "clientSecret cannot be blank");

    return new ClientCredentialsOAuthDancer(clientId, clientSecret, tokenUrl, scopes, encodeClientCredentialsInBody, encoding,
                                            responseAccessTokenExpr, responseRefreshTokenExpr, responseExpiresInExpr,
                                            customParametersExtractorsExprs, lockProvider, tokensStore,
                                            createHttpClient(httpService, tlsContextFactory), expressionEvaluator);
  }

}
