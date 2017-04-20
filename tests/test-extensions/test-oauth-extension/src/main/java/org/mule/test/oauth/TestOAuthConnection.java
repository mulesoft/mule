/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

public class TestOAuthConnection {

  private final TestOAuthConnectionState state;

  public TestOAuthConnection(TestOAuthConnectionState state) {
    this.state = state;
  }

  public TestOAuthConnectionState getState() {
    return state;
  }
}
