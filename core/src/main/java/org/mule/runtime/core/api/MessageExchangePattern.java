/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
