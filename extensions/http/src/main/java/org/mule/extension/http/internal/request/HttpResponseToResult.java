/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.SET_COOKIE;
import static org.mule.service.http.api.HttpHeaders.Names.SET_COOKIE2;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.decodeUrlEncodedBody;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.error.HttpMessageParsingException;
import org.mule.extension.http.internal.request.builder.HttpResponseAttributesBuilder;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that transforms an HTTP response to a proper {@link Result}.
 *
 * @since 4.0
 */
public class HttpResponseToResult {

  private static final Logger logger = LoggerFactory.getLogger(HttpResponseToResult.class);
  private static final String MULTI_PART_PREFIX = "multipart/";

  private final Boolean parseResponse;
  private final HttpRequesterCookieConfig config;
  private final MuleContext muleContext;

  public HttpResponseToResult(HttpRequesterCookieConfig config, Boolean parseResponse, MuleContext muleContext) {
    this.config = config;
    this.parseResponse = parseResponse;
    this.muleContext = muleContext;
  }

  public Result<Object, HttpResponseAttributes> convert(MediaType mediaType, HttpResponse response, String uri)
      throws HttpMessageParsingException {
    String responseContentType = response.getHeaderValueIgnoreCase(CONTENT_TYPE);
    if (isEmpty(responseContentType) && !ANY.matches(mediaType)) {
      responseContentType = mediaType.toRfcString();
    }

    InputStream responseInputStream = ((InputStreamHttpEntity) response.getEntity()).getInputStream();
    Charset encoding = getMediaType(responseContentType, getDefaultEncoding(muleContext)).getCharset().get();

    Object payload = responseInputStream;
    if (responseContentType != null && parseResponse) {
      if (responseContentType.startsWith(MULTI_PART_PREFIX)) {
        try {
          payload = multiPartPayloadForAttachments(responseContentType, responseInputStream);
        } catch (IOException e) {
          throw new HttpMessageParsingException(createStaticMessage("Unable to process multipart response"), e);
        }
      } else if (responseContentType.startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())) {
        payload = decodeUrlEncodedBody(IOUtils.toString(responseInputStream), encoding);
      }
    }

    if (config.isEnableCookies()) {
      processCookies(response, uri);
    }

    HttpResponseAttributes responseAttributes = createAttributes(response);

    mediaType = DataType.builder().mediaType(mediaType).charset(encoding).build().getMediaType();
    final Result.Builder builder = Result.builder().output(payload);

    if (isEmpty(responseContentType)) {
      builder.mediaType(mediaType);
    } else {
      builder.mediaType(MediaType.parse(responseContentType));
    }

    return builder.attributes(responseAttributes).build();
  }

  private static MultiPartPayload multiPartPayloadForAttachments(String responseContentType, InputStream responseInputStream)
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

      parts.add(Message.builder().payload(httpPart.getInputStream()).mediaType(MediaType.parse(httpPart.getContentType()))
          .attributes(new PartAttributes(httpPart.getName() != null ? httpPart.getName() : "part_" + partNumber,
                                         httpPart.getFileName(), httpPart.getSize(), headers))
          .build());

      partNumber++;
    }

    return new DefaultMultiPartPayload(parts);
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
        String[] contentDispositions = part.getHeader(CONTENT_DISPOSITION);
        if (contentDispositions != null) {
          String contentDisposition = contentDispositions[0];
          if (contentDisposition.contains("name")) {
            partName = contentDisposition.substring(contentDisposition.indexOf("name") + "name".length() + 2);
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

  private HttpResponseAttributes createAttributes(HttpResponse response) {
    return new HttpResponseAttributesBuilder().setResponse(response).build();
  }

  private void processCookies(HttpResponse response, String uri) {
    Collection<String> setCookieHeader = response.getHeaderValuesIgnoreCase(SET_COOKIE);
    Collection<String> setCookie2Header = response.getHeaderValuesIgnoreCase(SET_COOKIE2);

    Map<String, List<String>> cookieHeaders = new HashMap<>();

    if (setCookieHeader != null) {
      cookieHeaders.put(SET_COOKIE, new ArrayList<>(setCookieHeader));
    }

    if (setCookie2Header != null) {
      cookieHeaders.put(SET_COOKIE2, new ArrayList<>(setCookie2Header));
    }

    try {
      config.getCookieManager().put(URI.create(uri), cookieHeaders);
    } catch (IOException e) {
      logger.warn("Error storing cookies for URI " + uri, e);
    }
  }

  /**
   * 
   * @param contentTypeValue
   * @param defaultCharset the encoding to use if the given {@code contentTypeValue} doesn't have a {@code charset} parameter.
   * @return
   */
  private static MediaType getMediaType(final String contentTypeValue, Charset defaultCharset) {
    MediaType mediaType = MediaType.ANY;

    if (contentTypeValue != null) {
      try {
        mediaType = MediaType.parse(contentTypeValue);
      } catch (IllegalArgumentException e) {
        // need to support invalid Content-Types
        if (parseBoolean(System.getProperty(SYSTEM_PROPERTY_PREFIX + "strictContentType"))) {
          throw e;
        } else {
          logger.warn(format("%s when parsing Content-Type '%s': %s", e.getClass().getName(), contentTypeValue, e.getMessage()));
          logger.warn(format("Using default encoding: %s", defaultCharset().name()));
        }
      }
    }
    if (!mediaType.getCharset().isPresent()) {
      return mediaType.withCharset(defaultCharset);
    } else {
      return mediaType;
    }
  }
}
