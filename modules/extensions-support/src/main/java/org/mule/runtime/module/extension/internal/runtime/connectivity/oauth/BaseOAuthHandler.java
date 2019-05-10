/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.util.LazyLookup;
import org.mule.runtime.oauth.api.OAuthService;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

public abstract class BaseOAuthHandler<Dancer> implements Lifecycle {

  private static final Logger LOGGER = getLogger(BaseOAuthHandler.class);

  @Inject
  protected MuleContext muleContext;

  @Inject
  protected LockFactory lockFactory;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  protected ObjectStoreManager objectStoreManager;

  @Inject
  protected MuleExpressionLanguage expressionEvaluator;

  // TODO: MULE-10837 this should be a plain old @Inject
  protected LazyValue<OAuthService> oauthService;

  protected Function<OAuthConfig, ObjectStore> objectStoreLocator;


  protected final Map<String, Dancer> dancers = new ConcurrentHashMap<>();
  protected boolean started = false;

  protected Map<String, String> getParameterExtractors(OAuthConfig config) {
    Map<Field, String> extractors = config.getParameterExtractors();
    return extractors.entrySet().stream()
        .collect(toMap(entry -> entry.getKey().getName(), entry -> entry.getValue()));
  }

  @Override
  public void initialise() throws InitialisationException {
    oauthService = new LazyLookup<>(OAuthService.class, muleContext);
    objectStoreLocator = buildObjectStoreLocator();
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

  @Override
  public void dispose() {
    // no default action
  }

  private Function<OAuthConfig, ObjectStore> buildObjectStoreLocator() {
    return config -> {
      Optional<OAuthObjectStoreConfig> storeConfig = config.getStoreConfig();
      String storeName = storeConfig
          .map(OAuthObjectStoreConfig::getObjectStoreName)
          .orElse(BASE_PERSISTENT_OBJECT_STORE_KEY);

      return objectStoreManager.getObjectStore(storeName);
    };
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
