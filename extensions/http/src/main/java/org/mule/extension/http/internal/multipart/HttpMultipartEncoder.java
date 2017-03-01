/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.multipart;

import static java.lang.Math.toIntExact;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

/**
 * Creates multipart
 */
public class HttpMultipartEncoder {

  private static final String FORM_DATA = "form-data";
  public static final String ATTACHMENT = "attachment";

  public static MimeMultipart createMultpartContent(MultipartHttpEntity body, String contentType) {
    String contentTypeSubType = getContentTypeSubType(contentType);
    MimeMultipart mimeMultipartContent = new HttpMimeMultipart(contentType, contentTypeSubType);
    final Collection<HttpPart> parts = body.getParts();

    for (HttpPart part : parts) {
      final InternetHeaders internetHeaders = new InternetHeaders();
      for (String headerName : part.getHeaderNames()) {
        final Collection<String> headerValues = part.getHeaders(headerName);
        for (String headerValue : headerValues) {
          internetHeaders.addHeader(headerName, headerValue);
        }
      }
      if (internetHeaders.getHeader(CONTENT_DISPOSITION) == null) {
        String partType = contentTypeSubType.equals(FORM_DATA) ? FORM_DATA : ATTACHMENT;
        internetHeaders.addHeader(CONTENT_DISPOSITION, getContentDisposition(part, partType));
      }
      if (internetHeaders.getHeader(CONTENT_TYPE) == null && part.getContentType() != null) {
        internetHeaders.addHeader(CONTENT_TYPE, part.getContentType());
      }
      try {
        final byte[] partContent = IOUtils.toByteArray(part.getInputStream());
        mimeMultipartContent.addBodyPart(new MimeBodyPart(internetHeaders, partContent));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return mimeMultipartContent;
  }

  /**
   * Extracts the subtype from a content type
   *
   * @param contentType the content type
   * @return subtype of the content type.
   */
  private static String getContentTypeSubType(String contentType) {
    final ContentType contentTypeValue;
    try {
      contentTypeValue = new ContentType(contentType);
      return contentTypeValue.getSubType();
    } catch (ParseException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static String getContentDisposition(HttpPart part, String partType) {
    StringBuilder builder = new StringBuilder();
    builder.append(partType);
    builder.append("; name=\"");
    builder.append(part.getName());
    builder.append("\"");
    if (part.getFileName() != null) {
      builder.append("; filename=\"");
      builder.append(part.getFileName());
      builder.append("\"");
    }
    return builder.toString();
  }

  public static byte[] createMultipartContent(MultipartHttpEntity multipartEntity, String contentType)
      throws IOException, MessagingException {
    MimeMultipart mimeMultipartContent = HttpMultipartEncoder.createMultpartContent(multipartEntity, contentType);
    final ByteArrayOutputStream byteArrayOutputStream;
    byteArrayOutputStream = new ByteArrayOutputStream();
    mimeMultipartContent.writeTo(byteArrayOutputStream);
    return byteArrayOutputStream.toByteArray();
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

}
