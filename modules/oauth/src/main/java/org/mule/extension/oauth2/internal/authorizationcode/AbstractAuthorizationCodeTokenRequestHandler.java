/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import static java.util.Arrays.asList;
import static org.mule.extension.http.api.HttpConstants.Methods.GET;
import static org.mule.extension.oauth2.internal.DynamicFlowFactory.createDynamicFlow;
import static org.mule.extension.oauth2.internal.authorizationcode.RequestHandlerUtils.addRequestHandler;

import org.mule.extension.oauth2.internal.AbstractTokenRequestHandler;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.module.http.internal.listener.matcher.DefaultMethodRequestMatcher;
import org.mule.runtime.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.service.http.api.server.PathAndMethodRequestMatcher;
import org.mule.service.http.api.server.RequestHandlerManager;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Base class for token request handler.
 */
public abstract class AbstractAuthorizationCodeTokenRequestHandler extends AbstractTokenRequestHandler {

  private AuthorizationCodeGrantType oauthConfig;
  private RequestHandlerManager redirectUrlHandlerManager;


  /**
   * Updates the access token by calling the token url with refresh token grant type
   *
   * @param currentEvent the event at the moment of the failure.
   * @param resourceOwnerId the resource owner id to update
   */
  public void refreshToken(final Event currentEvent, String resourceOwnerId) throws MuleException {
    if (logger.isDebugEnabled()) {
      logger.debug("Executing refresh token for user " + resourceOwnerId);
    }
    final ResourceOwnerOAuthContext resourceOwnerOAuthContext =
        getOauthConfig().getUserOAuthContext().getContextForResourceOwner(resourceOwnerId);
    final boolean lockWasAcquired = resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().tryLock();
    try {
      if (lockWasAcquired) {
        doRefreshToken(currentEvent, resourceOwnerOAuthContext);
        getOauthConfig().getUserOAuthContext().updateResourceOwnerOAuthContext(resourceOwnerOAuthContext);
      }
    } finally {
      if (lockWasAcquired) {
        resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
      }
    }
    if (!lockWasAcquired) {
      // if we couldn't acquire the lock then we wait until the other thread updates the token.
      waitUntilLockGetsReleased(resourceOwnerOAuthContext);
    }
  }

  /**
   * ThreadSafe refresh token operation to be implemented by subclasses
   *
   * @param currentEvent the event at the moment of the failure.
   * @param resourceOwnerOAuthContext user oauth context object.
   */
  protected abstract void doRefreshToken(final Event currentEvent, final ResourceOwnerOAuthContext resourceOwnerOAuthContext)
      throws MuleException;

  private void waitUntilLockGetsReleased(ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().lock();
    resourceOwnerOAuthContext.getRefreshUserOAuthContextLock().unlock();
  }

  /**
   * @param oauthConfig oauth config for this token request handler.
   */
  public void setOauthConfig(AuthorizationCodeGrantType oauthConfig) {
    this.setTlsContextFactory(oauthConfig.getTlsContext());
    this.oauthConfig = oauthConfig;
  }

  public AuthorizationCodeGrantType getOauthConfig() {
    return oauthConfig;
  }

  /**
   * initialization method after configuration.
   */
  public void init() throws MuleException {}

  protected void createListenerForCallbackUrl() throws MuleException {
    String flowName = "OAuthCallbackUrlFlow";

    final PathAndMethodRequestMatcher requestMatcher;

    if (getOauthConfig().getLocalCallbackUrl() != null) {
      flowName = flowName + getOauthConfig().getLocalCallbackUrl();
      try {
        final URL localCallbackUrl = new URL(getOauthConfig().getLocalCallbackUrl());
        // TODO MULE-11283 improve this API
        requestMatcher = new ListenerRequestMatcher(new DefaultMethodRequestMatcher(GET.name()), localCallbackUrl.getPath());
      } catch (MalformedURLException e) {
        logger.warn("Could not parse provided url %s. Validate that the url is correct", getOauthConfig().getLocalCallbackUrl());
        throw new DefaultMuleException(e);
      }
      // TODO MULE-11276 - Need a way to reuse an http listener declared in the application/domain")
      // } else if (getOauthConfig().getLocalCallbackConfig() != null) {
      // flowName =
      // flowName + getOauthConfig().getLocalCallbackConfig().getName() + "_" + getOauthConfig().getLocalCallbackConfigPath();
      // requestMatcher =
      // new ListenerRequestMatcher(new DefaultMethodRequestMatcher(GET.name()), getOauthConfig().getLocalCallbackConfigPath());
    } else {
      throw new IllegalStateException("No localCallbackUrl or localCallbackConfig defined.");
    }

    final Flow redirectUrlFlow = createDynamicFlow(getMuleContext(), flowName, asList(createRedirectUrlProcessor()));

    this.redirectUrlHandlerManager =
        addRequestHandler(getOauthConfig().getServer(), requestMatcher, redirectUrlFlow, logger);
  }

  @Override
  public void start() throws MuleException {
    redirectUrlHandlerManager.start();
    super.start();
  }

  @Override
  public void stop() {
    super.stop();
    redirectUrlHandlerManager.stop();
  }

  protected abstract Processor createRedirectUrlProcessor();

}
