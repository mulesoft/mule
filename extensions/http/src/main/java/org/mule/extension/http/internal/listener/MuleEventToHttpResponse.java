/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.http.internal.listener;

import static org.mule.extension.http.api.HttpStreamingType.ALWAYS;
import static org.mule.extension.http.api.HttpStreamingType.AUTO;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.getReasonPhraseForStatusCode;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;

import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.listener.builder.HttpListenerResponseBuilder;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.runtime.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder;
import org.mule.runtime.module.http.internal.listener.HttpResponseHeaderBuilder;
import org.mule.runtime.module.http.internal.multipart.HttpMultipartEncoder;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that transforms a {@link MuleEvent} to an {@link HttpResponse}.
 *
 * @since 4.0
 */
public class MuleEventToHttpResponse {

  public static final String MULTIPART = "multipart";
  private Logger logger = LoggerFactory.getLogger(getClass());

  private HttpStreamingType responseStreaming = AUTO;
  private boolean multipartEntityWithNoMultipartContentyTypeWarned;
  private boolean mapPayloadButNoUrlEncodedContentyTypeWarned;

  public MuleEventToHttpResponse(HttpStreamingType responseStreaming) {
    this.responseStreaming = responseStreaming;
  }

  /**
   * Creates an {@HttpResponse}.
   *
   * @param event The {@link MuleEvent} that should be used to set the {@link HttpResponse} content.
   * @param responseBuilder The {@link HttpResponseBuilder} that should be modified if necessary and used to build the
   *        {@link HttpResponse}.
   * @param listenerResponseBuilder The generic {@HttpListenerResponseBuilder} configured for this listener.
   * @param supportsTransferEncoding boolean that determines whether the HTTP protocol of the response supports streaming.
   * @return an {@HttpResponse} configured based on the parameters.
   * @throws MessagingException if the response creation fails.
   */
  public HttpResponse create(MuleEvent event, HttpResponseBuilder responseBuilder,
                             HttpListenerResponseBuilder listenerResponseBuilder, boolean supportsTransferEncoding)
      throws MessagingException {
    Map<String, String> headers = listenerResponseBuilder.getHeaders(event);

    final HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();

    for (String name : headers.keySet()) {
      // For now, only support single headers
      if (TRANSFER_ENCODING.equals(name) && !supportsTransferEncoding) {
        logger
            .debug("Client HTTP version is lower than 1.1 so the unsupported 'Transfer-Encoding' header has been removed and 'Content-Length' will be sent instead.");
      } else {
        httpResponseHeaderBuilder.addHeader(name, headers.get(name));
      }
    }

    if (httpResponseHeaderBuilder.getContentType() == null) {
      DataType dataType = event.getMessage().getDataType();
      if (!MediaType.ANY.matches(dataType.getMediaType())) {
        httpResponseHeaderBuilder.addHeader(CONTENT_TYPE, dataType.getMediaType().toString());
      }
    }

    final String configuredContentType = httpResponseHeaderBuilder.getContentType();
    final String existingTransferEncoding = httpResponseHeaderBuilder.getTransferEncoding();
    final String existingContentLength = httpResponseHeaderBuilder.getContentLength();

    HttpEntity httpEntity;

    Map<String, DataHandler> parts = listenerResponseBuilder.getParts(event);

    if (!parts.isEmpty()) {
      if (configuredContentType == null) {
        httpResponseHeaderBuilder.addContentType(createMultipartFormDataContentType());
      } else if (!configuredContentType.startsWith(MULTIPART)) {
        warnNoMultipartContentTypeButMultipartEntity(httpResponseHeaderBuilder.getContentType());
      }
      httpEntity = createMultipartEntity(event, httpResponseHeaderBuilder.getContentType(), parts);
      resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding,
                      (ByteArrayHttpEntity) httpEntity);
    } else {
      final Object payload = event.getMessage().getPayload();
      if (payload == null) {
        setupContentLengthEncoding(httpResponseHeaderBuilder, 0);
        httpEntity = new EmptyHttpEntity();
      } else if (payload instanceof Map) {
        if (configuredContentType == null) {
          httpResponseHeaderBuilder.addContentType(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());
        } else if (!configuredContentType.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())) {
          warnMapPayloadButNoUrlEncodedContentType(httpResponseHeaderBuilder.getContentType());
        }
        httpEntity = createUrlEncodedEntity(event, (Map) payload);
        if (responseStreaming == ALWAYS && supportsTransferEncoding) {
          setupChunkedEncoding(httpResponseHeaderBuilder);
        } else {
          if (httpEntity instanceof EmptyHttpEntity) {
            setupContentLengthEncoding(httpResponseHeaderBuilder, 0);
          } else {
            ByteArrayHttpEntity byteArrayHttpEntity = (ByteArrayHttpEntity) httpEntity;
            setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
          }
        }
      } else if (payload instanceof InputStream) {
        if (responseStreaming == ALWAYS || (responseStreaming == AUTO && existingContentLength == null)) {
          if (supportsTransferEncoding) {
            setupChunkedEncoding(httpResponseHeaderBuilder);
          }
          httpEntity = new InputStreamHttpEntity((InputStream) payload);
        } else {
          ByteArrayHttpEntity byteArrayHttpEntity = new ByteArrayHttpEntity(IOUtils.toByteArray(((InputStream) payload)));
          setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
          httpEntity = byteArrayHttpEntity;
        }
      } else {
        try {
          ByteArrayHttpEntity byteArrayHttpEntity = new ByteArrayHttpEntity(event.getMessageAsBytes());
          resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding,
                          byteArrayHttpEntity);
          httpEntity = byteArrayHttpEntity;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    Collection<String> headerNames = httpResponseHeaderBuilder.getHeaderNames();
    for (String headerName : headerNames) {
      Collection<String> values = httpResponseHeaderBuilder.getHeader(headerName);
      for (String value : values) {
        responseBuilder.addHeader(headerName, value);
      }
    }

    Integer statusCode = listenerResponseBuilder.getStatusCode(event);
    if (statusCode != null) {
      responseBuilder.setStatusCode(statusCode);
    }
    String reasonPhrase = resolveReasonPhrase(listenerResponseBuilder.getReasonPhrase(event), statusCode);
    if (reasonPhrase != null) {
      responseBuilder.setReasonPhrase(reasonPhrase);
    }
    responseBuilder.setEntity(httpEntity);
    return responseBuilder.build();
  }

  public String resolveReasonPhrase(String builderReasonPhrase, Integer statusCode) {
    String reasonPhrase = builderReasonPhrase;
    if (reasonPhrase == null && statusCode != null) {
      reasonPhrase = getReasonPhraseForStatusCode(statusCode);
    }
    return reasonPhrase;
  }

  private void resolveEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder, String existingTransferEncoding,
                               String existingContentLength, boolean supportsTransferEncoding,
                               ByteArrayHttpEntity byteArrayHttpEntity) {
    if (responseStreaming == ALWAYS
        || (responseStreaming == AUTO && existingContentLength == null && CHUNKED.equals(existingTransferEncoding))) {
      if (supportsTransferEncoding) {
        setupChunkedEncoding(httpResponseHeaderBuilder);
      }
    } else {
      setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
    }
  }

  private void setupContentLengthEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder, int contentLength) {
    if (httpResponseHeaderBuilder.getTransferEncoding() != null) {
      logger.debug("Content-Length encoding is being used so the 'Transfer-Encoding' header has been removed");
      httpResponseHeaderBuilder.removeHeader(TRANSFER_ENCODING);
    }
    httpResponseHeaderBuilder.setContentLenght(String.valueOf(contentLength));
  }

  private void setupChunkedEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder) {
    if (httpResponseHeaderBuilder.getContentLength() != null) {
      logger.debug("Chunked encoding is being used so the 'Content-Length' header has been removed");
      httpResponseHeaderBuilder.removeHeader(CONTENT_LENGTH);
    }
    httpResponseHeaderBuilder.addHeader(TRANSFER_ENCODING, CHUNKED);
  }

  private String createMultipartFormDataContentType() {
    return String.format("%s; boundary=%s", HttpHeaders.Values.MULTIPART_FORM_DATA, UUID.getUUID());
  }

  private HttpEntity createUrlEncodedEntity(MuleEvent event, Map payload) {
    final Map mapPayload = payload;
    HttpEntity entity = new EmptyHttpEntity();
    if (!mapPayload.isEmpty()) {
      String encodedBody;
      final Charset encoding = event.getMessage().getDataType().getMediaType().getCharset().get();
      if (mapPayload instanceof ParameterMap) {
        encodedBody = HttpParser.encodeString(encoding, ((ParameterMap) mapPayload).toListValuesMap());
      } else {
        encodedBody = HttpParser.encodeString(encoding, mapPayload);
      }
      entity = new ByteArrayHttpEntity(encodedBody.getBytes());
    }
    return entity;
  }

  private void warnMapPayloadButNoUrlEncodedContentType(String contentType) {
    if (!mapPayloadButNoUrlEncodedContentyTypeWarned) {
      logger.warn(String.format(
                                "Payload is a Map which will be used to generate an url encoded http body but Contenty-Type specified is %s and not %s.",
                                contentType, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED));
      mapPayloadButNoUrlEncodedContentyTypeWarned = true;
    }
  }

  private void warnNoMultipartContentTypeButMultipartEntity(String contentType) {
    if (!multipartEntityWithNoMultipartContentyTypeWarned) {
      logger.warn(String.format(
                                "Sending http response with Content-Type %s but the message has attachment and a multipart entity is generated.",
                                contentType));
      multipartEntityWithNoMultipartContentyTypeWarned = true;
    }
  }

  private HttpEntity createMultipartEntity(MuleEvent event, String contentType, Map<String, DataHandler> parts)
      throws MessagingException {
    if (logger.isDebugEnabled()) {
      logger.debug("Message contains attachments. Ignoring payload and trying to generate multipart response.");
    }

    final MultipartHttpEntity multipartEntity;
    try {
      multipartEntity = new MultipartHttpEntity(HttpPartDataSource.createFrom(parts));
      return new ByteArrayHttpEntity(HttpMultipartEncoder.createMultipartContent(multipartEntity, contentType));
    } catch (Exception e) {
      throw new MessagingException(MessageFactory.createStaticMessage("Error creating multipart HTTP entity."),
                                   event.getMessage(), event.getMuleContext(), e);
    }
  }

}
