/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.stream.Collectors.toMap;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.oauth.api.OAuthService;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class BaseOAuthHandler implements Startable, Stoppable {

  protected final LazyValue<HttpService> httpService;
  protected final LazyValue<OAuthService> oauthService;
  protected final LockFactory lockFactory;

  public BaseOAuthHandler(LazyValue<HttpService> httpService,
                          LazyValue<OAuthService> oauthService, LockFactory lockFactory) {
    this.httpService = httpService;
    this.oauthService = oauthService;
    this.lockFactory = lockFactory;
  }

  protected Map<String, String> getParameterExtractors(OAuthConfig config) {
    Map<Field, String> extractors = config.getParameterExtractors();
    return extractors.entrySet().stream()
        .collect(toMap(entry -> entry.getKey().getName(), entry -> entry.getValue()));
  }
}
