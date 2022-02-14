/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message;

import org.mule.runtime.http2.api.message.content.Http2Content;

public class BaseHttp2Request extends BaseHttp2Message implements Http2Request {

  private final Http2Method method;

  public BaseHttp2Request(Http2Method method, Http2Content content) {
    super(content);
    this.method = method;
  }

  public BaseHttp2Request(CharSequence methodAsText, Http2Content content) {
    this(Http2Method.fromText(methodAsText), content);
  }

  @Override
  public Http2Method getMethod() {
    return method;
  }
}
