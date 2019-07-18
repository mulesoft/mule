/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.listener;

/**
 * Allows to get notified about events related to the state of an OAuth authorization
 * @since 4.2.2 - 4.3.0
 */
public interface OAuthStateListener {

  /**
   * Invoked when a tokens gets invalidated by a Mule application. Not to be confused with revocation at the service
   * provider's end.
   */
  default void onTokenInvalidated() {

  }
}
