/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
