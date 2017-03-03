/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static org.mule.runtime.core.util.StringUtils.WHITE_SPACE;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;

import org.mule.service.http.api.domain.entity.multipart.HttpPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;

public class HttpParser {

  private static final String SPACE_ENTITY = "%20";
  private static final String PLUS_SIGN = "\\+";
  private static final String CONTENT_DISPOSITION_PART_HEADER = CONTENT_DISPOSITION;
  private static final String NAME_ATTRIBUTE = "name";

  public static String extractPath(String uri) {
    String path = uri;
    int i = path.indexOf('?');
    if (i > -1) {
      path = path.substring(0, i);
    }
    return path;
  }

  public static Collection<HttpPart> parseMultipartContent(InputStream content, String contentType) throws IOException {
    MimeMultipart mimeMultipart = null;
    List<HttpPart> parts = Lists.newArrayList();

    try {
      mimeMultipart = new MimeMultipart(new ByteArrayDataSource(content, contentType));
    } catch (MessagingException e) {
      throw new IOException(e);
    }

    try {
      int partCount = mimeMultipart.getCount();

      for (int i = 0; i < partCount; i++) {
        BodyPart part = mimeMultipart.getBodyPart(i);

        String filename = part.getFileName();
        String partName = filename;
        String[] contentDispositions = part.getHeader(CONTENT_DISPOSITION_PART_HEADER);
        if (contentDispositions != null) {
          String contentDisposition = contentDispositions[0];
          if (contentDisposition.contains(NAME_ATTRIBUTE)) {
            partName = contentDisposition.substring(contentDisposition.indexOf(NAME_ATTRIBUTE) + NAME_ATTRIBUTE.length() + 2);
            partName = partName.substring(0, partName.indexOf("\""));
          }
        }
        HttpPart httpPart =
            new HttpPart(partName, filename, IOUtils.toByteArray(part.getInputStream()), part.getContentType(), part.getSize());

        Enumeration<Header> headers = part.getAllHeaders();

        while (headers.hasMoreElements()) {
          Header header = headers.nextElement();
          httpPart.addHeader(header.getName(), header.getValue());
        }
        parts.add(httpPart);
      }
    } catch (MessagingException e) {
      throw new IOException(e);
    }

    return parts;
  }

  /**
   * Normalize a path that may contains spaces, %20 or +.
   *
   * @param path path with encoded spaces or raw spaces
   * @return path with only spaces.
   */
  public static String normalizePathWithSpacesOrEncodedSpaces(String path) {
    return path.replaceAll(SPACE_ENTITY, WHITE_SPACE).replaceAll(PLUS_SIGN, WHITE_SPACE);
  }
}
