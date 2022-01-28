/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.client;

public class Http2ClientConfiguration {

  public static Builder builder() {
    return new Builder();
  }

  private Http2ClientConfiguration() {}

  public static class Builder {

    private Http2ClientConfiguration product;

    private Builder() {
      product = new Http2ClientConfiguration();
    }

    public Http2ClientConfiguration build() {
      return product;
    }
  }
}
