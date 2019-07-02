/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.state;

/**
 * Represents the possible states that the owner object may be in relative to the Dancer.
 *
 * @since 4.3, 4.2.2
 */
public enum DancerState {

  /**
   * The owner has not fetched a token yet, or a previous attempt to fetch it has failed.
   */
  NO_TOKEN,

  /**
   * There is already a request executing to refresh the token.
   */
  REFRESHING_TOKEN,

  /**
   * There is a token present and it is valid to use.
   */
  HAS_TOKEN

}
