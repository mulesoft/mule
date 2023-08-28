/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

public enum MessageExchangePattern {

  ONE_WAY {

    @Override
    public boolean hasResponse() {
      return false;
    }
  },

  REQUEST_RESPONSE {

    @Override
    public boolean hasResponse() {
      return true;
    }
  };

  public abstract boolean hasResponse();

  public static MessageExchangePattern fromString(String string) {
    String mepString = string.toUpperCase().replace('-', '_');
    return MessageExchangePattern.valueOf(mepString);
  }
}
