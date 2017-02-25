/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.decodeUrlEncodedBody;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.error.HttpMessageParsingException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.HttpPart;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.request.HttpRequestContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that transforms an HTTP request to a proper {@link Result}.
 *
 * @since 4.0
 */
public class HttpRequestToResult {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestToResult.class);

  public static Result<Object, HttpRequestAttributes> transform(final HttpRequestContext requestContext,
                                                                final Charset encoding,
                                                                Boolean parseRequest,
                                                                ListenerPath listenerPath)
      throws HttpMessageParsingException {
    final HttpRequest request = requestContext.getRequest();

    final MediaType mediaType = getMediaType(request.getHeaderValueIgnoreCase(CONTENT_TYPE), encoding);

    Object payload = null;
    if (parseRequest) {
      final HttpEntity entity = request.getEntity();
      if (entity != null && !(entity instanceof EmptyHttpEntity)) {
        if (entity instanceof MultipartHttpEntity) {
          try {
            payload = multiPartPayloadForAttachments((MultipartHttpEntity) entity);
          } catch (IOException e) {
            throw new HttpMessageParsingException(createStaticMessage("Unable to process multipart request"), e);
          }
        } else {
          if (mediaType != null) {
            if (mediaType.matches(APPLICATION_X_WWW_FORM_URLENCODED)) {
              try {
                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()),
                                               mediaType.getCharset().get());
              } catch (IllegalArgumentException e) {
                throw new HttpMessageParsingException(createStaticMessage("Cannot decode %s payload",
                                                                          APPLICATION_X_WWW_FORM_URLENCODED.getSubType()),
                                                      e);
              }
            } else if (entity instanceof InputStreamHttpEntity) {
              payload = ((InputStreamHttpEntity) entity).getInputStream();
            }
          } else if (entity instanceof InputStreamHttpEntity) {
            payload = ((InputStreamHttpEntity) entity).getInputStream();
          }
        }
      }
    } else {
      final InputStreamHttpEntity inputStreamEntity = request.getInputStreamEntity();
      if (inputStreamEntity != null) {
        payload = inputStreamEntity.getInputStream();
      }
    }

    HttpRequestAttributes attributes =
        new HttpRequestAttributesBuilder().setRequestContext(requestContext).setListenerPath(listenerPath).build();

    return Result.<Object, HttpRequestAttributes>builder().output(payload).mediaType(mediaType).attributes(attributes).build();
  }

  public static MultiPartPayload multiPartPayloadForAttachments(MultipartHttpEntity entity) throws IOException {
    return multiPartPayloadForAttachments(entity.getParts());
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

  /**
   * 
   * @param contentTypeValue
   * @param defaultCharset the encoding to use if the given {@code contentTypeValue} doesn't have a {@code charset} parameter.
   * @return
   */
  public static MediaType getMediaType(final String contentTypeValue, Charset defaultCharset) {
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
