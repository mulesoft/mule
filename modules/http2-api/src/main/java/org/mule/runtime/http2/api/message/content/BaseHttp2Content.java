/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message.content;

import java.io.InputStream;

public abstract class BaseHttp2Content implements Http2Content {

  private final InputStream asInputStream;

  public BaseHttp2Content(InputStream asInputStream) {
    this.asInputStream = asInputStream;
  }

  @Override
  public InputStream asInputStream() {
    return asInputStream;
  }
}
