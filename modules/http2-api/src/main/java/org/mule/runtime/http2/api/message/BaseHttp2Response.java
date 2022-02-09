/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message;

import org.mule.runtime.http2.api.message.content.Http2Content;

public class BaseHttp2Response extends BaseHttp2Message implements Http2Response {

  private final Http2Status http2Status;

  public BaseHttp2Response(Http2Status http2Status, Http2Content content) {
    super(content);
    this.http2Status = http2Status;
  }

  @Override
  public Http2Status getStatus() {
    return http2Status;
  }
}
