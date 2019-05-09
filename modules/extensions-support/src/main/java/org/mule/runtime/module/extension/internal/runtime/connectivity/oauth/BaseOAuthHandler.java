/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.oauth.api.OAuthService;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;

public abstract class BaseOAuthHandler<Dancer> implements Startable, Stoppable {

  private static final Logger LOGGER = getLogger(BaseOAuthHandler.class);

  protected final LazyValue<HttpService> httpService;
  protected final LazyValue<OAuthService> oauthService;
  protected final MuleExpressionLanguage expressionEvaluator;
  protected final Function<OAuthConfig, ObjectStore> objectStoreLocator;
  protected final LockFactory lockFactory;
  protected final MuleContext muleContext;

  protected final Map<String, Dancer> dancers = new ConcurrentHashMap<>();
  protected boolean started = false;

  public BaseOAuthHandler(LazyValue<HttpService> httpService,
                          LazyValue<OAuthService> oauthService,
                          LockFactory lockFactory,
                          MuleExpressionLanguage expressionEvaluator,
                          Function<OAuthConfig, ObjectStore> objectStoreLocator,
                          MuleContext muleContext) {
    this.httpService = httpService;
    this.oauthService = oauthService;
    this.lockFactory = lockFactory;
    this.expressionEvaluator = expressionEvaluator;
    this.objectStoreLocator = objectStoreLocator;
    this.muleContext = muleContext;
  }

  protected Map<String, String> getParameterExtractors(OAuthConfig config) {
    Map<Field, String> extractors = config.getParameterExtractors();
    return extractors.entrySet().stream()
        .collect(toMap(entry -> entry.getKey().getName(), entry -> entry.getValue()));
  }

  @Override
  public void start() throws MuleException {
    for (Dancer dancer : dancers.values()) {
      start(dancer);
    }
    started = true;
  }

  protected void start(Dancer dancer) throws MuleException {
    initialiseIfNeeded(dancer, muleContext);
    startIfNeeded(dancer);
  }

  @Override
  public void stop() throws MuleException {
    dancers.forEach((key, dancer) -> {
      try {
        disable(key, dancer);
      } catch (Exception e) {
        LOGGER.warn("Found exception while trying to stop OAuth dancer for config " + key, e);
      }
    });
    dancers.clear();
  }

  private void disable(String ownerConfigName, Dancer dancer) {
    try {
      stopIfNeeded(dancer);
    } catch (Exception e) {
      LOGGER.warn("Found exception trying to Stop OAuth dancer for config " + ownerConfigName, e);
    } finally {
      disposeIfNeeded(dancer, LOGGER);
    }
  }
}
