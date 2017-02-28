/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.http.internal.multipart;

import static java.lang.Math.toIntExact;
import static java.util.stream.Collectors.toList;
import static org.mule.compatibility.module.http.internal.HttpParser.parseMultipartContent;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.message.ds.ByteArrayDataSource;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;

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

  public static Collection<HttpPart> createFrom(MultiPartPayload multiPartPayload, Transformer objectToByteArray) {
    return multiPartPayload.getParts().stream().map(message -> {
      PartAttributes partAttributes = (PartAttributes) message.getAttributes();
      TypedValue<Object> payload = message.getPayload();
      String name = partAttributes.getName();
      byte[] data;
      try {
        data = (byte[]) objectToByteArray.transform(payload.getValue());
        String fileName = partAttributes.getFileName();
        String contentType = payload.getDataType().getMediaType().toRfcString();
        int size = toIntExact(partAttributes.getSize());
        if (fileName != null) {
          return new HttpPart(name, fileName, data, contentType, size);
        } else {
          return new HttpPart(name, data, contentType, size);
        }
      } catch (TransformerException e) {
        throw new MuleRuntimeException(createStaticMessage(String.format("Could not create HTTP part %s", name), e));
      }
    }).collect(toList());
  }

  public static MultiPartPayload multiPartPayloadForAttachments(MultipartHttpEntity entity) throws IOException {
    return multiPartPayloadForAttachments(entity.getParts());
  }

  public static MultiPartPayload multiPartPayloadForAttachments(String responseContentType, InputStream responseInputStream)
      throws IOException {
    return multiPartPayloadForAttachments(parseMultipartContent(responseInputStream, responseContentType));
  }

  private static MultiPartPayload multiPartPayloadForAttachments(Collection<HttpPart> httpParts) throws IOException {
    List<org.mule.runtime.api.message.Message> parts = new ArrayList<>();

    int partNumber = 1;
    for (HttpPart httpPart : httpParts) {
      Map<String, LinkedList<String>> headers = new HashMap<>();
      for (String headerName : httpPart.getHeaderNames()) {
        if (!headers.containsKey(headerName)) {
          headers.put(headerName, new LinkedList<>());
        }
        headers.get(headerName).addAll(httpPart.getHeaders(headerName));
      }

      parts.add(InternalMessage.builder().payload(httpPart.getInputStream()).mediaType(MediaType.parse(httpPart.getContentType()))
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
