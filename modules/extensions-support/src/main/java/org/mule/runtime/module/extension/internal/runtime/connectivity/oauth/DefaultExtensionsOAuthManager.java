/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.artifact.Registry;
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
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.oauth.api.OAuthService;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * Default implementation of {@link ExtensionsOAuthManager}
 *
 * @since 4.0
 */
public class DefaultExtensionsOAuthManager implements Lifecycle, ExtensionsOAuthManager {

  private static final Logger LOGGER = getLogger(DefaultExtensionsOAuthManager.class);

  @Inject
  private MuleContext muleContext;

  @Inject
  private LockFactory lockFactory;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  @Inject
  private MuleExpressionLanguage expressionEvaluator;

  @Inject
  private Registry registry;

  // TODO: MULE-10837 this should be a plain old @Inject
  private LazyValue<HttpService> httpService;

  // TODO: MULE-10837 this should be a plain old @Inject
  private LazyValue<OAuthService> oauthService;

  private AuthorizationCodeOAuthHandler authCodeHandler;

  @Override
  public AuthorizationCodeOAuthHandler getAuthorizationCodeOAuthHandler() {
    return authCodeHandler;
  }

  @Override
  public void initialise() throws InitialisationException {
    httpService = new LazyLookup<>(HttpService.class, muleContext);
    oauthService = new LazyLookup<>(OAuthService.class, muleContext);

    authCodeHandler = new AuthorizationCodeOAuthHandler(httpService,
                                                        oauthService,
                                                        lockFactory,
                                                        expressionEvaluator,
                                                        getObjectStoreLocator(),
                                                        registry,
                                                        muleContext);

    initialiseIfNeeded(authCodeHandler);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(authCodeHandler);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(authCodeHandler);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(authCodeHandler, LOGGER);
  }

  private Function<OAuthConfig, ObjectStore> getObjectStoreLocator() {
    return config -> {
      Optional<OAuthObjectStoreConfig> storeConfig = config.getStoreConfig();
      String storeName = storeConfig
          .map(OAuthObjectStoreConfig::getObjectStoreName)
          .orElse(BASE_PERSISTENT_OBJECT_STORE_KEY);

      return objectStoreManager.getObjectStore(storeName);
    };
  }
}
