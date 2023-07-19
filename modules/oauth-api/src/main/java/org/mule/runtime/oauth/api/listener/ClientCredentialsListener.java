/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.oauth.api.listener;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;

/**
 * Allows to get notified about certain events related to an OAuth dance with Client Credentials grant type
 * 
 * @since 4.2.1
 * @deprecated since 4.2.2. Use {@link org.mule.oauth.client.api.listener.ClientCredentialsListener} instead
 */
@Deprecated
public interface ClientCredentialsListener extends org.mule.oauth.client.api.listener.ClientCredentialsListener,
    OAuthStateListener {

  /**
   * Invoked each time a refresh token operation has been completed successfully
   *
   * @param context the resulting {@link ResourceOwnerOAuthContext}
   */
  @Override
  void onTokenRefreshed(ResourceOwnerOAuthContext context);

  default void onTokenRefreshed(org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext context) {}
}
