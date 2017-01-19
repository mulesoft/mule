/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.service.Service;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 * Allows for creation of {@link OAuthDancer} implementations for the supported grant types:
 * <ul>
 * <li>Authorization Code Grant Type</li>
 * <li>Client Credentials</li>
 * </ul>
 *
 * @since 4.0
 */
public interface OAuthService extends Service {

  /**
   * Creates an {@link OAuthDancer} for client credentials grant type. The dancer will use the given {@code lockProvider} and
   * {@code tokensStore} to manage its internal state.
   * 
   * @param config the configuration for the returned {@link OAuthDancer dancer}
   * @param lockProvider a factory for {@link Lock}s, uniquely identified by the {@code name} passed to the {@link Function}.
   * @param tokensStore the repository for the tokens for the returned {@link OAuthDancer dancer}.
   * @param expressionEvaluator the object used to evaluate expressions.
   * 
   * @return a client-credentials grant type dancer.
   */
  <T> OAuthDancer createClientCredentialsGrantTypeDancer(ClientCredentialsConfig config, Function<String, Lock> lockProvider,
                                                         Map<String, T> tokensStore, ExpressionEvaluator expressionEvaluator);

  /**
   * Creates an {@link OAuthDancer} for authorization code grant type. The dancer will use the given {@code lockProvider} and
   * {@code tokensStore} to manage its internal state.
   * 
   * @param config the configuration for the returned {@link OAuthDancer dancer}
   * @param lockProvider a factory for {@link Lock}s, uniquely identified by the {@code name} passed to the {@link Function}.
   * @param tokensStore the repository for the tokens for the returned {@link OAuthDancer dancer}.
   * @param expressionEvaluator the object used to evaluate expressions.
   * @return an authorization-code grant type dancer.
   */
  <T> OAuthDancer createAuthorizationCodeGrantTypeDancer(AuthorizationCodeOAuthConfig config, Function<String, Lock> lockProvider,
                                                         Map<String, T> tokensStore, ExpressionEvaluator expressionEvaluator);

}
