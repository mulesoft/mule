/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of a {@link Part}.
 */
public class HttpPart implements Part {

  public static final int NO_SIZE = -1;

  private final byte[] content;
  private final String contentType;
  private final String partName;
  private final String fileName;
  private final int size;
  private Map<String, Object> headers = new HashMap<>();

  public HttpPart(String partName, byte[] content, String contentType, int size) {
    this(partName, null, content, contentType, size);
  }

  public HttpPart(String partName, String fileName, byte[] content, String contentType, int size) {
    this.partName = partName;
    this.fileName = fileName;
    this.content = content;
    this.contentType = contentType;
    this.size = size;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public String getHeader(String headerName) {
    return (String) headers.get(headerName);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  @Override
  public Collection<String> getHeaders(String headerName) {
    Object headerValue = headers.get(headerName);

    if (headerValue instanceof Collection) {
      return (Collection<String>) headerValue;
    } else {
      List<String> headerList = new ArrayList<>();
      headerList.add((String) headerValue);
      return headerList;
    }
  }

  public void addHeader(String headerName, String headerValue) {
    final Object value = headers.get(headerName);
    if (value == null) {
      headers.put(headerName, headerValue);
    } else {
      if (value instanceof Collection) {
        ((Collection) value).add(headerValue);
      } else {
        final ArrayList<String> values = new ArrayList<String>();
        values.add((String) value);
        values.add(headerValue);
        headers.put(headerName, values);
      }
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(content);
  }

  @Override
  public String getName() {
    return partName;
  }

  @Override
  public long getSize() {
    return size;
  }

  public String getFileName() {
    return fileName;
  }

}
