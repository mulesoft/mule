/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.internal.HttpParser.decodeUrlEncodedBody;
import static org.mule.runtime.module.http.internal.multipart.HttpPartDataSource.multiPartPayloadForAttachments;
import static org.mule.runtime.module.http.internal.util.HttpToMuleMessage.getMediaType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestToMuleEvent {

  public static InternalMessage transform(final HttpRequestContext requestContext, final Charset charset,
                                          Boolean parseRequest, ListenerPath listenerPath)
      throws HttpRequestParsingException {
    final HttpRequest request = requestContext.getRequest();
    final Collection<String> headerNames = request.getHeaderNames();
    Map<String, Serializable> inboundProperties = new HashMap<>();
    Map<String, Serializable> outboundProperties = new HashMap<>();
    for (String headerName : headerNames) {
      // Content-Type was already processed
      if (!CONTENT_TYPE.equalsIgnoreCase(headerName)) {
        final Collection<String> values = request.getHeaderValues(headerName);
        if (values.size() == 1) {
          inboundProperties.put(headerName, values.iterator().next());
        } else {
          inboundProperties.put(headerName, new ArrayList<>(values));
        }
      }
    }

    new HttpMessagePropertiesResolver().setMethod(request.getMethod()).setProtocol(request.getProtocol().asString())
        .setUri(request.getUri()).setListenerPath(listenerPath).setRemoteHostAddress(resolveRemoteHostAddress(requestContext))
        .setScheme(requestContext.getScheme()).setClientCertificate(requestContext.getClientConnection().getClientCertificate())
        .addPropertiesTo(inboundProperties);

    Object payload = null;

    final MediaType mediaType = getMediaType(request.getHeaderValueIgnoreCase(CONTENT_TYPE), charset);
    if (parseRequest) {
      final HttpEntity entity = request.getEntity();
      if (entity != null && !(entity instanceof EmptyHttpEntity)) {
        if (entity instanceof MultipartHttpEntity) {
          try {
            payload = multiPartPayloadForAttachments((MultipartHttpEntity) entity);
          } catch (IOException e) {
            throw new HttpRequestParsingException(e.getMessage(), e);
          }
        } else {
          if (mediaType != null) {
            if (mediaType.matches(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
              try {
                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()),
                                               mediaType.getCharset().get());
              } catch (IllegalArgumentException e) {
                throw new HttpRequestParsingException("Cannot decode x-www-form-urlencoded payload", e);
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

    final InternalMessage message =
        InternalMessage.builder().payload(payload).mediaType(mediaType).inboundProperties(inboundProperties)
            .outboundProperties(outboundProperties).build();
    // TODO does a correlation id come as a header that we may use?
    return message;
  }

  private static String resolveRemoteHostAddress(final HttpRequestContext requestContext) {
    return requestContext.getClientConnection().getRemoteHostAddress().toString();
  }
}
