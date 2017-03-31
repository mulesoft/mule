/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.extension.http.api.error.HttpError.SECURITY;
import static org.mule.extension.http.api.error.HttpError.TRANSFORMATION;
import static org.mule.extension.http.internal.multipart.HttpMultipartEncoder.createFrom;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.COOKIE;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeString;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.internal.HttpStreamingType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;

import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that generates {@link HttpRequest HttpRequests}.
 *
 * @since 4.0
 */
public class HttpRequestFactory {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestFactory.class);
  public static final List<String> DEFAULT_EMPTY_BODY_METHODS = Lists.newArrayList("GET", "HEAD", "OPTIONS");
  private static final String APPLICATION_JAVA = "application/java";

  private final String uri;
  private final String method;
  private final HttpRequesterCookieConfig config;
  private final HttpStreamingType streamingMode;
  private final HttpSendBodyMode sendBodyMode;
  private final TransformationService transformationService;


  public HttpRequestFactory(HttpRequesterCookieConfig config, String uri, String method, HttpStreamingType streamingMode,
                            HttpSendBodyMode sendBodyMode, TransformationService transformationService) {
    this.config = config;
    this.uri = uri;
    this.method = method;
    this.streamingMode = streamingMode;
    this.sendBodyMode = sendBodyMode;
    this.transformationService = transformationService;
  }

  /**
   * Creates an {@HttpRequest}.
   *
   * @param requestBuilder The generic {@link HttpRequesterRequestBuilder} from the request component that should be used to
   *        create the {@link HttpRequest}.
   * @param authentication The {@link HttpAuthentication} that should be used to create the {@link HttpRequest}.
   * @param muleContext the Mule node.
   * @return an {@HttpRequest} configured based on the parameters.
   * @throws MuleException if the request creation fails.
   */
  public HttpRequest create(HttpRequesterRequestBuilder requestBuilder, HttpAuthentication authentication,
                            MuleContext muleContext) {
    HttpRequestBuilder builder = HttpRequest.builder();

    builder.setUri(this.uri);
    builder.setMethod(this.method);
    builder.setHeaders(toParameterMap(requestBuilder.getHeaders()));
    builder.setQueryParams(toParameterMap(requestBuilder.getQueryParams()));

    MediaType mediaType = requestBuilder.getBody().getDataType().getMediaType();
    if (!builder.getHeaderValue(CONTENT_TYPE).isPresent()) {
      if (!MediaType.ANY.matches(mediaType)) {
        builder.addHeader(CONTENT_TYPE, mediaType.toRfcString());
      }
    }

    if (config.isEnableCookies()) {
      try {
        Map<String, List<String>> headers =
            config.getCookieManager().get(URI.create(uri), Collections.<String, List<String>>emptyMap());
        List<String> cookies = headers.get(COOKIE);
        if (cookies != null) {
          for (String cookie : cookies) {
            builder.addHeader(COOKIE, cookie);
          }
        }
      } catch (IOException e) {
        logger.warn("Error reading cookies for URI " + uri, e);
      }

    }

    try {
      builder.setEntity(createRequestEntity(builder, this.method, muleContext, requestBuilder.getBody().getValue(), mediaType));
    } catch (TransformerException e) {
      throw new ModuleException(e, TRANSFORMATION);
    }

    if (authentication != null) {
      try {
        authentication.authenticate(builder);
      } catch (MuleException e) {
        throw new ModuleException(e, SECURITY);
      }
    }

    return builder.build();
  }

  private ParameterMap toParameterMap(Map<String, String> map) {
    ParameterMap parameterMap = new ParameterMap();
    map.forEach(parameterMap::put);
    return parameterMap;
  }

  private HttpEntity createRequestEntity(HttpRequestBuilder requestBuilder, String resolvedMethod,
                                         MuleContext muleContext, Object body, MediaType mediaType)
      throws TransformerException {
    HttpEntity entity;

    if (isEmptyBody(body, resolvedMethod)) {
      entity = new EmptyHttpEntity();
    } else {
      entity = createRequestEntityFromPayload(requestBuilder, body, muleContext, mediaType);
    }

    return entity;
  }

  private boolean isEmptyBody(Object body, String method) {
    boolean emptyBody;

    // TODO MULE-9986 Use multi-part payload
    if (body == null) {
      emptyBody = true;
    } else {
      emptyBody = DEFAULT_EMPTY_BODY_METHODS.contains(method);

      if (sendBodyMode != HttpSendBodyMode.AUTO) {
        emptyBody = (sendBodyMode == HttpSendBodyMode.NEVER);
      }
    }

    return emptyBody;
  }

  private HttpEntity createRequestEntityFromPayload(HttpRequestBuilder requestBuilder, Object payload, MuleContext muleContext,
                                                    MediaType mediaType)
      throws TransformerException {
    if (payload instanceof MultiPartPayload) {
      Transformer objectToByteArray = muleContext.getRegistry().lookupTransformer(OBJECT, BYTE_ARRAY);
      return new MultipartHttpEntity(createFrom((MultiPartPayload) payload, objectToByteArray));
    }

    if (doStreaming(requestBuilder, payload)) {

      if (payload instanceof InputStream) {
        return new InputStreamHttpEntity((InputStream) payload);
      } else {
        return new InputStreamHttpEntity(new ByteArrayInputStream(getMessageAsBytes(payload)));
      }

    } else {
      Optional<String> contentType = requestBuilder.getHeaderValue(CONTENT_TYPE);

      if (!contentType.isPresent() || contentType.get().startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
          || contentType.get().startsWith(APPLICATION_JAVA)) {
        if (payload instanceof Map) {
          String body = encodeString((Map) payload, mediaType.getCharset()
              .orElse(getDefaultEncoding(muleContext)));
          requestBuilder.addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());
          return new ByteArrayHttpEntity(body.getBytes());
        }
      }

      return new ByteArrayHttpEntity(getMessageAsBytes(payload));
    }
  }

  private byte[] getMessageAsBytes(Object payload) throws TransformerException {
    return (byte[]) transformationService.transform(of(payload), BYTE_ARRAY).getPayload().getValue();
  }

  private boolean doStreaming(HttpRequestBuilder requestBuilder, Object payload) {
    Optional<String> transferEncodingHeader = requestBuilder.getHeaderValue(TRANSFER_ENCODING);
    Optional<String> contentLengthHeader = requestBuilder.getHeaderValue(CONTENT_LENGTH);

    if (streamingMode == HttpStreamingType.AUTO) {
      if (contentLengthHeader.isPresent()) {
        if (transferEncodingHeader.isPresent()) {
          requestBuilder.removeHeader(TRANSFER_ENCODING);

          if (logger.isDebugEnabled()) {
            logger.debug("Cannot send both Transfer-Encoding and Content-Length headers. Transfer-Encoding will not be sent.");
          }
        }
        return false;
      }

      if (!transferEncodingHeader.isPresent() || !transferEncodingHeader.get().equalsIgnoreCase(CHUNKED)) {
        return payload instanceof InputStream;
      } else {
        return true;
      }
    } else if (streamingMode == HttpStreamingType.ALWAYS) {
      if (contentLengthHeader != null) {
        requestBuilder.removeHeader(CONTENT_LENGTH);

        if (logger.isDebugEnabled()) {
          logger.debug("Content-Length header will not be sent, as the configured requestStreamingMode is ALWAYS");
        }
      }

      if (transferEncodingHeader.isPresent() && !transferEncodingHeader.get().equalsIgnoreCase(CHUNKED)) {
        requestBuilder.removeHeader(TRANSFER_ENCODING);

        if (logger.isDebugEnabled()) {
          logger.debug("Transfer-Encoding header will be sent with value 'chunked' instead of {}, as the configured "
              + "requestStreamingMode is NEVER", transferEncodingHeader);
        }

      }
      return true;
    } else {
      if (transferEncodingHeader != null) {
        requestBuilder.removeHeader(TRANSFER_ENCODING);

        if (logger.isDebugEnabled()) {
          logger.debug("Transfer-Encoding header will not be sent, as the configured requestStreamingMode is NEVER");
        }
      }
      return false;
    }
  }
}
