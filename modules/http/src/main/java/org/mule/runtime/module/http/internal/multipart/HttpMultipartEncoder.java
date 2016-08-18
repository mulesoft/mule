/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.multipart;

import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.runtime.core.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

/**
 * Creates multipart
 */
public class HttpMultipartEncoder {

  private static final String FORM_DATA = "form-data";
  public static final String ATTACHMENT = "attachment";

  public static MimeMultipart createMultpartContent(MultipartHttpEntity body, String contentType) {
    String contentTypeSubType = HttpParser.getContentTypeSubType(contentType);
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
}
