/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.muletosdk;

import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;
import org.mule.sdk.api.http.domain.entity.multipart.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class HttpPartWrapper implements Part {

  private final HttpPart mulePart;

  public HttpPartWrapper(HttpPart mulePart) {
    this.mulePart = mulePart;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return mulePart.getInputStream();
  }

  @Override
  public String getContentType() {
    return mulePart.getContentType();
  }

  @Override
  public String getName() {
    return mulePart.getName();
  }

  @Override
  public long getSize() {
    return mulePart.getSize();
  }

  @Override
  public String getHeader(String name) {
    return mulePart.getHeader(name);
  }

  @Override
  public Collection<String> getHeaders(String name) {
    return mulePart.getHeaders(name);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return mulePart.getHeaderNames();
  }

  @Override
  public String getFileName() {
    return mulePart.getFileName();
  }
}
