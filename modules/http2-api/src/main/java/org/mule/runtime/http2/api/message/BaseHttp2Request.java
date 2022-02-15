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
  private final String path;

  public BaseHttp2Request(Http2Method method, String path, Http2Content content) {
    super(content);
    this.method = method;
    this.path = path;
  }

  public BaseHttp2Request(String methodAsText, String path, Http2Content content) {
    this(Http2Method.fromText(methodAsText), path, content);
  }

  @Override
  public Http2Method getMethod() {
    return method;
  }

  @Override
  public String getPath() {
    return path;
  }
}
