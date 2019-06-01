/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

/**
 * Allows to get notified about certain events related to an OAuth dance with Client Credentials grant type
 * @since 4.2.1
 */
public interface ClientCredentialsListener {

  /**
   * Invoked each time a refresh token operation has been completed successfully
   *
   * @param context the resulting {@link ResourceOwnerOAuthContext}
   */
  default void onTokenRefreshed(ResourceOwnerOAuthContext context) {

  }
}
