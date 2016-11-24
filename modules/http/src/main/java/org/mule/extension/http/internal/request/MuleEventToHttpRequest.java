/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.COOKIE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.authentication.HttpAuthentication;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.internal.request.validator.HttpRequesterConfig;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;

import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that transforms a {@link Event} to a {@link HttpRequest}.
 *
 * @since 4.0
 */
public class MuleEventToHttpRequest {

  private static final Logger logger = LoggerFactory.getLogger(MuleEventToHttpRequest.class);
  public static final List<String> DEFAULT_EMPTY_BODY_METHODS = Lists.newArrayList("GET", "HEAD", "OPTIONS");
  private static final String APPLICATION_JAVA = "application/java";

  private final String uri;
  private final String method;
  private final HttpRequesterConfig config;
  private final HttpStreamingType streamingMode;
  private final HttpSendBodyMode sendBodyMode;
  private final TransformationService transformationService;


  public MuleEventToHttpRequest(HttpRequesterConfig config, String uri, String method, HttpStreamingType streamingMode,
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
   * @param event The {@link Event} that should be used to set the {@link HttpRequest} content.
   * @param requestBuilder The generic {@link HttpRequesterRequestBuilder} from the request component that should be used to
   *        create the {@link HttpRequest}.
   * @param authentication The {@link HttpAuthentication} that should be used to create the {@link HttpRequest}.
   * @param muleContext the Mule node.
   * @return an {@HttpRequest} configured based on the parameters.
   * @throws MuleException if the request creation fails.
   */
  public HttpRequest create(Event event, HttpRequesterRequestBuilder requestBuilder, HttpAuthentication authentication,
                            MuleContext muleContext)
      throws MuleException {
    HttpRequestBuilder builder = new HttpRequestBuilder();

    builder.setUri(this.uri);
    builder.setMethod(this.method);
    builder.setHeaders(toParameterMap(requestBuilder.getHeaders()));
    builder.setQueryParams(toParameterMap(requestBuilder.getQueryParams()));

    MediaType mediaType = requestBuilder.getMediaType();
    if (!builder.getHeaders().containsKey(CONTENT_TYPE)) {
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

    builder.setEntity(createRequestEntity(builder, event, this.method, muleContext, requestBuilder.getBody(), mediaType));

    if (authentication != null) {
      authentication.authenticate(event, builder);
    }

    return builder.build();
  }

  private ParameterMap toParameterMap(Map<String, String> map) {
    ParameterMap parameterMap = new ParameterMap();
    map.forEach(parameterMap::put);
    return parameterMap;
  }

  private HttpEntity createRequestEntity(HttpRequestBuilder requestBuilder, Event muleEvent, String resolvedMethod,
                                         MuleContext muleContext, Object body, MediaType mediaType)
      throws MessagingException {
    HttpEntity entity;

    if (isEmptyBody(body, resolvedMethod)) {
      entity = new EmptyHttpEntity();
    } else {
      entity = createRequestEntityFromPayload(requestBuilder, muleEvent, body, muleContext, mediaType);
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

  private HttpEntity createRequestEntityFromPayload(HttpRequestBuilder requestBuilder, Event muleEvent, Object payload,
                                                    MuleContext muleContext, MediaType mediaType)
      throws MessagingException {

    if (payload instanceof MultiPartPayload) {
      try {
        Transformer objectToByteArray = muleContext.getRegistry().lookupTransformer(OBJECT, BYTE_ARRAY);
        return new MultipartHttpEntity(HttpPartDataSource.createFrom((MultiPartPayload) payload, objectToByteArray));
      } catch (Exception e) {
        throw new MessagingException(muleEvent, e);
      }
    }

    if (doStreaming(requestBuilder, payload)) {

      if (payload instanceof InputStream) {
        return new InputStreamHttpEntity((InputStream) payload);
      } else {
        try {
          return new InputStreamHttpEntity(new ByteArrayInputStream(getMessageAsBytes(payload)));
        } catch (Exception e) {
          throw new MessagingException(muleEvent, e);
        }
      }

    } else {
      String contentType = requestBuilder.getHeaders().get(CONTENT_TYPE);

      if (contentType == null || contentType.startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
          || contentType.startsWith(APPLICATION_JAVA)) {
        if (payload instanceof Map) {
          String body = HttpParser.encodeString(mediaType.getCharset()
              .orElse(getDefaultEncoding(muleContext)), (Map) payload);
          requestBuilder.addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());
          return new ByteArrayHttpEntity(body.getBytes());
        }
      }

      try {
        return new ByteArrayHttpEntity(getMessageAsBytes(payload));
      } catch (Exception e) {
        throw new MessagingException(muleEvent, e);
      }
    }
  }

  private byte[] getMessageAsBytes(Object payload) {
    try {
      return (byte[]) transformationService.transform(InternalMessage.of(payload), BYTE_ARRAY).getPayload().getValue();
    } catch (TransformerException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private boolean doStreaming(HttpRequestBuilder requestBuilder, Object payload) throws MessagingException {
    String transferEncodingHeader = requestBuilder.getHeaders().get(TRANSFER_ENCODING);
    String contentLengthHeader = requestBuilder.getHeaders().get(CONTENT_LENGTH);

    if (streamingMode == HttpStreamingType.AUTO) {
      if (contentLengthHeader != null) {
        if (transferEncodingHeader != null) {
          requestBuilder.removeHeader(TRANSFER_ENCODING);

          if (logger.isDebugEnabled()) {
            logger.debug("Cannot send both Transfer-Encoding and Content-Length headers. Transfer-Encoding will not be sent.");
          }
        }
        return false;
      }

      if (transferEncodingHeader == null || !transferEncodingHeader.equalsIgnoreCase(CHUNKED)) {
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

      if (transferEncodingHeader != null && !transferEncodingHeader.equalsIgnoreCase(CHUNKED)) {
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
