/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message.content;

import java.io.ByteArrayInputStream;

public class EmptyHttp2Content extends BaseHttp2Content {

  private static final byte[] EMPTY_BUF = new byte[0];

  public EmptyHttp2Content() {
    super(new ByteArrayInputStream(EMPTY_BUF), 0);
  }
}
