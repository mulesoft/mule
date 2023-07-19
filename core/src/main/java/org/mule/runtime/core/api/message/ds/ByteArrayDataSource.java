/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.message.ds;


import org.mule.runtime.api.metadata.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public final class ByteArrayDataSource implements DataSource {

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
