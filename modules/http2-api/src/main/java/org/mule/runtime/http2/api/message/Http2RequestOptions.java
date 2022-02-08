/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message;

public class Http2RequestOptions {

  public static Builder builder() {
    return new Builder();
  }

  private Http2RequestOptions() {}

  public static class Builder {

    private Http2RequestOptions product;

    private Builder() {
      product = new Http2RequestOptions();
    }

    public Http2RequestOptions build() {
      return product;
    }
  }
}
