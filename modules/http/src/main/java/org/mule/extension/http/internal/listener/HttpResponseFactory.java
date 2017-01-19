/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.http.internal.listener;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.getReasonPhraseForStatusCode;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.listener.builder.HttpListenerResponseBuilder;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.listener.HttpResponseHeaderBuilder;
import org.mule.runtime.module.http.internal.multipart.HttpMultipartEncoder;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.domain.message.response.HttpResponseBuilder;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that creates {@link HttpResponse HttpResponses}.
 *
 * @since 4.0
 */
public class HttpResponseFactory {

  public static final String MULTIPART = "multipart";
  private Logger logger = LoggerFactory.getLogger(getClass());

  private HttpStreamingType responseStreaming = HttpStreamingType.AUTO;
  private boolean multipartEntityWithNoMultipartContentyTypeWarned;
  private boolean mapPayloadButNoUrlEncodedContentTypeWarned;
  private TransformationService transformationService;
  private final Transformer objectToByteArray = new ObjectToByteArray();

  public HttpResponseFactory(HttpStreamingType responseStreaming,
                             TransformationService transformationService) {
    this.responseStreaming = responseStreaming;
    this.transformationService = transformationService;
  }

  /**
   * Creates an {@HttpResponse}.
   *
   * @param responseBuilder The {@link HttpResponseBuilder} that should be modified if necessary and used to build the
   *        {@link HttpResponse}.
   * @param listenerResponseBuilder The generic {@HttpListenerResponseBuilder} configured for this listener.
   * @param supportsTransferEncoding boolean that determines whether the HTTP protocol of the response supports streaming.
   * @return an {@HttpResponse} configured based on the parameters.
   * @throws MessagingException if the response creation fails.
   */
  public HttpResponse create(HttpResponseBuilder responseBuilder,
                             HttpListenerResponseBuilder listenerResponseBuilder,
                             boolean supportsTransferEncoding)
      throws MessagingException {

    Map<String, String> headers = listenerResponseBuilder.getHeaders();

    final HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();

    headers.forEach((key, value) -> {
      // For now, only support single headers
      if (TRANSFER_ENCODING.equals(key) && !supportsTransferEncoding) {
        logger.debug(
                     "Client HTTP version is lower than 1.1 so the unsupported 'Transfer-Encoding' header has been removed and 'Content-Length' will be sent instead.");
      } else {
        httpResponseHeaderBuilder.addHeader(key, value);
      }
    });

    if (httpResponseHeaderBuilder.getContentType() == null && !ANY.matches(listenerResponseBuilder.getMediaType())) {
      httpResponseHeaderBuilder.addHeader(CONTENT_TYPE, listenerResponseBuilder.getMediaType().toString());
    }

    final String configuredContentType = httpResponseHeaderBuilder.getContentType();
    final String existingTransferEncoding = httpResponseHeaderBuilder.getTransferEncoding();
    final String existingContentLength = httpResponseHeaderBuilder.getContentLength();

    HttpEntity httpEntity;
    final Object payload = listenerResponseBuilder.getBody();

    if (payload == null) {
      setupContentLengthEncoding(httpResponseHeaderBuilder, 0);
      httpEntity = new EmptyHttpEntity();
    } else if (payload instanceof Map) {
      if (configuredContentType == null) {
        httpResponseHeaderBuilder.addContentType(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());
      } else if (!configuredContentType.startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())) {
        warnMapPayloadButNoUrlEncodedContentType(httpResponseHeaderBuilder.getContentType());
      }
      httpEntity = createUrlEncodedEntity(listenerResponseBuilder.getMediaType(), (Map) payload);
      if (responseStreaming == HttpStreamingType.ALWAYS && supportsTransferEncoding) {
        setupChunkedEncoding(httpResponseHeaderBuilder);
      } else {
        if (httpEntity instanceof EmptyHttpEntity) {
          setupContentLengthEncoding(httpResponseHeaderBuilder, 0);
        } else {
          ByteArrayHttpEntity byteArrayHttpEntity = (ByteArrayHttpEntity) httpEntity;
          setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
        }
      }
    } else if (payload instanceof MultiPartPayload) {
      if (configuredContentType == null) {
        httpResponseHeaderBuilder.addContentType(createMultipartFormDataContentType());
      } else if (!configuredContentType.startsWith(MULTIPART)) {
        warnNoMultipartContentTypeButMultipartEntity(httpResponseHeaderBuilder.getContentType());
      }
      httpEntity = createMultipartEntity(httpResponseHeaderBuilder.getContentType(), (MultiPartPayload) payload);
      resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding,
                      (ByteArrayHttpEntity) httpEntity);
    } else if (payload instanceof InputStream) {
      if (responseStreaming == HttpStreamingType.ALWAYS
          || (responseStreaming == HttpStreamingType.AUTO && existingContentLength == null)) {
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
      ByteArrayHttpEntity byteArrayHttpEntity = new ByteArrayHttpEntity(getMessageAsBytes(payload));

      resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding,
                      byteArrayHttpEntity);
      httpEntity = byteArrayHttpEntity;
    }

    Collection<String> headerNames = httpResponseHeaderBuilder.getHeaderNames();
    for (String headerName : headerNames) {
      Collection<String> values = httpResponseHeaderBuilder.getHeader(headerName);
      for (String value : values) {
        responseBuilder.addHeader(headerName, value);
      }
    }

    Integer statusCode = listenerResponseBuilder.getStatusCode();
    if (statusCode != null) {
      responseBuilder.setStatusCode(statusCode);
    }
    String reasonPhrase = resolveReasonPhrase(listenerResponseBuilder.getReasonPhrase(), statusCode);
    if (reasonPhrase != null) {
      responseBuilder.setReasonPhrase(reasonPhrase);
    }
    responseBuilder.setEntity(httpEntity);
    return responseBuilder.build();
  }

  private byte[] getMessageAsBytes(Object payload) {
    try {
      return (byte[]) transformationService.transform(InternalMessage.of(payload), BYTE_ARRAY).getPayload().getValue();
    } catch (TransformerException e) {
      throw new MuleRuntimeException(e);
    }
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
    if (responseStreaming == HttpStreamingType.ALWAYS
        || (responseStreaming == HttpStreamingType.AUTO && existingContentLength == null
            && CHUNKED.equals(existingTransferEncoding))) {
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
    String existingTransferEncoding = httpResponseHeaderBuilder.getTransferEncoding();
    if (!CHUNKED.equals(existingTransferEncoding)) {
      httpResponseHeaderBuilder.addHeader(TRANSFER_ENCODING, CHUNKED);
    }
  }

  private String createMultipartFormDataContentType() {
    return String.format("%s; boundary=%s", HttpHeaders.Values.MULTIPART_FORM_DATA, UUID.getUUID());
  }

  private HttpEntity createUrlEncodedEntity(MediaType mediaType, Map payload) {
    final Map mapPayload = payload;
    HttpEntity entity = new EmptyHttpEntity();
    if (!mapPayload.isEmpty()) {
      String encodedBody;
      final Charset encoding = mediaType.getCharset().get();
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
    if (!mapPayloadButNoUrlEncodedContentTypeWarned) {
      logger.warn(String.format(
                                "Payload is a Map which will be used to generate an url encoded http body but Contenty-Type specified is %s and not %s.",
                                contentType, APPLICATION_X_WWW_FORM_URLENCODED));
      mapPayloadButNoUrlEncodedContentTypeWarned = true;
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

  private HttpEntity createMultipartEntity(String contentType, MultiPartPayload partPayload)
      throws MessagingException {
    if (logger.isDebugEnabled()) {
      logger.debug("Message contains attachments. Ignoring payload and trying to generate multipart response.");
    }

    final MultipartHttpEntity multipartEntity;
    try {

      multipartEntity = new MultipartHttpEntity(HttpPartDataSource.createFrom(partPayload, objectToByteArray));
      return new ByteArrayHttpEntity(HttpMultipartEncoder.createMultipartContent(multipartEntity, contentType));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Error creating multipart HTTP entity."), e);
    }
  }

}
