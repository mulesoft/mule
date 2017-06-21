/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message.ds;


import org.mule.runtime.api.metadata.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class ByteArrayDataSource implements DataSource {

  private final byte[] data;
  private final MediaType contentType;
  private final String name;

  public ByteArrayDataSource(byte[] data, MediaType contentType, String name) {
    this.data = data;
    this.contentType = contentType;
    this.name = name;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(data);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new IOException("Cannot write into a ByteArrayDataSource");
  }

  @Override
  public String getContentType() {
    return contentType.toString();
  }

  @Override
  public String getName() {
    return name;
  }
}
