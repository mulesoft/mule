/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.service.http.api.HttpHeaders.Names.AUTHORIZATION;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.util.store.ObjectStoreToMapAdapter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

import java.util.concurrent.ExecutionException;

/**
 * Authorization element for client credentials oauth grant type
 * <p>
 * The first token refresh will be done when the application is started.
 */
@XmlHints(allowTopLevelDefinition = true, allowInlineDefinition = false)
public class ClientCredentialsGrantType extends AbstractGrantType {

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean encodeClientCredentialsInBody;

  @Override
  protected OAuthDancerBuilder configDancer(OAuthService oauthService) {
    OAuthClientCredentialsDancerBuilder dancerBuilder =
        oauthService.clientCredentialsGrantTypeDancerBuilder(lockId -> muleContext.getLockFactory().createLock(lockId),
                                                             new ObjectStoreToMapAdapter(tokenManager.getObjectStore()),
                                                             muleContext.getExpressionManager());
    return dancerBuilder.encodeClientCredentialsInBody(encodeClientCredentialsInBody);
  }

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    try {
      builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken(DEFAULT_RESOURCE_OWNER_ID).get()));
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new DefaultMuleException(e);
    } catch (ExecutionException e) {
      throw new DefaultMuleException(e.getCause());
    }
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) throws MuleException {
    final Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      try {
        dancer.refreshToken(DEFAULT_RESOURCE_OWNER_ID).get();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        throw new DefaultMuleException(e);
      } catch (ExecutionException e) {
        throw new DefaultMuleException(e.getCause());
      }
    }
    return shouldRetryRequest;
  }
}
