/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.multipart;

import static org.mule.runtime.module.http.internal.HttpParser.parseMultipartContent;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.ds.ByteArrayDataSource;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

public class HttpPartDataSource implements DataSource {

  private final HttpPart part;
  private byte[] content;

  private HttpPartDataSource(HttpPart part) {
    try {
      this.part = part;
      this.content = IOUtils.toByteArray(part.getInputStream());
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public byte[] getContent() throws IOException {
    return this.content;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(getContent());
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContentType() {
    return part.getContentType();
  }

  public String getHeader(String headerName) {
    return part.getHeader(headerName);
  }

  @Override
  public String getName() {
    return part.getName();
  }

  public static Collection<HttpPart> createFrom(Map<String, DataHandler> parts) throws IOException {
    final ArrayList<HttpPart> httpParts = new ArrayList<>(parts.size());
    for (String partName : parts.keySet()) {
      final DataHandler dataHandlerPart = parts.get(partName);
      if (dataHandlerPart.getDataSource() instanceof HttpPartDataSource) {
        httpParts.add(((HttpPartDataSource) dataHandlerPart.getDataSource()).getPart());
      } else {
        byte[] data = IOUtils.toByteArray(dataHandlerPart.getInputStream());
        String fileName = null;

        if (dataHandlerPart.getDataSource() instanceof FileDataSource
            || dataHandlerPart.getDataSource() instanceof ByteArrayDataSource) {
          fileName = dataHandlerPart.getDataSource().getName();
        }
        httpParts.add(new HttpPart(partName, fileName, data, dataHandlerPart.getContentType(), data.length));
      }
    }
    return httpParts;
  }

  public static MultiPartPayload multiPartPayloadForAttachments(MultipartHttpEntity entity) throws IOException {
    return multiPartPayloadForAttachments(entity.getParts());
  }

  public static MultiPartPayload multiPartPayloadForAttachments(String responseContentType, InputStream responseInputStream)
      throws IOException {
    return multiPartPayloadForAttachments(parseMultipartContent(responseInputStream, responseContentType));
  }

  private static MultiPartPayload multiPartPayloadForAttachments(Collection<HttpPart> httpParts) throws IOException {
    List<org.mule.runtime.api.message.MuleMessage> parts = new ArrayList<>();

    int partNumber = 1;
    for (HttpPart httpPart : httpParts) {
      Map<String, LinkedList<String>> headers = new HashMap<>();
      for (String headerName : httpPart.getHeaderNames()) {
        if (!headers.containsKey(headerName)) {
          headers.put(headerName, new LinkedList<>());
        }
        headers.get(headerName).addAll(httpPart.getHeaders(headerName));
      }

      parts.add(MuleMessage.builder().payload(httpPart.getInputStream()).mediaType(MediaType.parse(httpPart.getContentType()))
          .attributes(new PartAttributes(httpPart.getName() != null ? httpPart.getName() : "part_" + partNumber,
                                         httpPart.getFileName(), httpPart.getSize(), headers))
          .build());

      partNumber++;
    }

    return new DefaultMultiPartPayload(parts);
  }

  public HttpPart getPart() {
    return part;
  }
}
