/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Allows for creation of OAuth dancer implementations for the supported grant types:
 * <ul>
 * <li>Authorization Code Grant Type</li>
 * <li>Client Credentials</li>
 * </ul>
 *
 * @since 4.0
 */
public interface OAuthService extends Service {

  /**
   * Creates a builder for an {@link ClientCredentialsOAuthDancer} for client credentials grant type. The dancer will use the
   * given {@code lockProvider} and {@code tokensStore} to manage its internal state.
   * 
   * @param lockProvider a factory for {@link Lock}s, uniquely identified by the {@code name} passed to
   *        {@link LockFactory#createLock(String)}.
   * @param tokensStore the repository for the tokens for the returned {@link ClientCredentialsOAuthDancer dancer}.
   * @param expressionEvaluator the object used to evaluate expressions.
   * 
   * @return a builder for a client-credentials grant type dancer.
   */
  <T> OAuthClientCredentialsDancerBuilder clientCredentialsGrantTypeDancerBuilder(LockFactory lockProvider,
                                                                                  Map<String, T> tokensStore,
                                                                                  MuleExpressionLanguage expressionEvaluator);

  /**
   * Creates a builder for an {@link AuthorizationCodeOAuthDancer} for authorization code grant type. The dancer will use the
   * given {@code lockProvider} and {@code tokensStore} to manage its internal state.
   * 
   * @param lockProvider a factory for {@link Lock}s, uniquely identified by the {@code name} passed to
   *        {@link LockFactory#createLock(String)}.
   * @param tokensStore the repository for the tokens for the returned {@link AuthorizationCodeOAuthDancer dancer}.
   * @param expressionEvaluator the object used to evaluate expressions.
   * 
   * @return a builder for an authorization-code grant type dancer.
   */
  <T> OAuthAuthorizationCodeDancerBuilder authorizationCodeGrantTypeDancerBuilder(LockFactory lockProvider,
                                                                                  Map<String, T> tokensStore,
                                                                                  MuleExpressionLanguage expressionEvaluator);

}
