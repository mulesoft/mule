/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.entity;

import java.io.InputStream;

public class InputStreamHttpEntity implements HttpEntity {

  private Integer contentLength;
  private InputStream inputStream;

  public InputStreamHttpEntity(Integer contentLength, InputStream inputStream) {
    this.contentLength = contentLength;
    this.inputStream = inputStream;
  }

  public InputStreamHttpEntity(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public int getContentLength() {
    return this.contentLength;
  }

  public boolean hasContentLength() {
    return this.contentLength != null;
  }

  public InputStream getInputStream() {
    return this.inputStream;
  }
}
