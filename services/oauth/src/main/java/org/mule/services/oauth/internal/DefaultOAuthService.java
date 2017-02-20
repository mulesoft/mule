/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal;

import org.mule.runtime.api.el.ExpressionEvaluator;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpService;
import org.mule.services.oauth.internal.builder.DefaultOAuthAuthorizationCodeDancerBuilder;
import org.mule.services.oauth.internal.builder.DefaultOAuthClientCredentialsDancerBuilder;

import java.util.Map;


public final class DefaultOAuthService implements OAuthService {

  private final HttpService httpService;
  private final OAuthCallbackServersManager httpServersManager;
  private final SchedulerService schedulerService;

  public DefaultOAuthService(HttpService httpService, SchedulerService schedulerService) {
    this.httpService = httpService;
    httpServersManager = new OAuthCallbackServersManager(httpService);
    this.schedulerService = schedulerService;
  }

  @Override
  public String getName() {
    return "OAuthService";
  }

  @Override
  public <T> OAuthClientCredentialsDancerBuilder clientCredentialsGrantTypeDancerBuilder(LockFactory lockProvider,
                                                                                         Map<String, T> tokensStore,
                                                                                         ExpressionEvaluator expressionEvaluator) {
    return new DefaultOAuthClientCredentialsDancerBuilder(lockProvider, (Map<String, ResourceOwnerOAuthContext>) tokensStore,
                                                          httpService, expressionEvaluator);
  }

  @Override
  public <T> OAuthAuthorizationCodeDancerBuilder authorizationCodeGrantTypeDancerBuilder(LockFactory lockProvider,
                                                                                         Map<String, T> tokensStore,
                                                                                         ExpressionEvaluator expressionEvaluator) {
    return new DefaultOAuthAuthorizationCodeDancerBuilder(httpServersManager, schedulerService, lockProvider,
                                                          (Map<String, ResourceOwnerOAuthContext>) tokensStore,
                                                          httpService, expressionEvaluator);
  }
}
