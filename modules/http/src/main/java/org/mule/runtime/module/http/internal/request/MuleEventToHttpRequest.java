/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.service.http.api.HttpConstants.RequestProperties.HTTP_PREFIX;
import static org.mule.service.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.COOKIE;
import static org.mule.service.http.api.HttpHeaders.Names.HOST;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.runtime.module.http.internal.request.DefaultHttpRequester.DEFAULT_EMPTY_BODY_METHODS;
import static org.mule.runtime.module.http.internal.request.DefaultHttpRequester.DEFAULT_PAYLOAD_EXPRESSION;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.api.requester.HttpSendBodyMode;
import org.mule.runtime.module.http.api.requester.HttpStreamingType;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;

import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.activation.DataHandler;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MuleEventToHttpRequest {

  private static final Logger logger = LoggerFactory.getLogger(MuleEventToHttpRequest.class);
  private static final List<String> ignoredProperties = Arrays.asList(CONNECTION, HOST, TRANSFER_ENCODING);
  private static final String APPLICATION_JAVA = "application/java";

  private DefaultHttpRequester requester;
  private MuleContext muleContext;

  private AttributeEvaluator requestStreamingMode;
  private AttributeEvaluator sendBody;


  public MuleEventToHttpRequest(DefaultHttpRequester requester, MuleContext muleContext, AttributeEvaluator requestStreamingMode,
                                AttributeEvaluator sendBody) {
    this.requester = requester;
    this.muleContext = muleContext;
    this.requestStreamingMode = requestStreamingMode;
    this.sendBody = sendBody;
  }

  public HttpRequestBuilder create(Event event, String resolvedMethod, String resolvedUri) throws MessagingException {
    HttpRequesterRequestBuilder requestBuilder = requester.getRequestBuilder();
    HttpRequestBuilder builder = HttpRequest.builder();

    builder.setUri(resolvedUri);
    builder.setMethod(resolvedMethod);
    builder.setHeaders(requestBuilder.getHeaders(event, muleContext));
    builder.setQueryParams(requestBuilder.getQueryParams(event, muleContext));

    for (String outboundProperty : event.getMessage().getOutboundPropertyNames()) {
      if (isNotIgnoredProperty(outboundProperty)) {
        builder.addHeader(outboundProperty, event.getMessage().getOutboundProperty(outboundProperty).toString());
      }
    }

    DataType dataType = event.getMessage().getPayload().getDataType();
    if (!MediaType.ANY.matches(dataType.getMediaType())) {
      builder.addHeader(CONTENT_TYPE, dataType.getMediaType().toRfcString());
    }

    if (requester.getConfig().isEnableCookies()) {
      try {
        Map<String, List<String>> headers =
            requester.getConfig().getCookieManager().get(URI.create(resolvedUri), Collections.<String, List<String>>emptyMap());
        List<String> cookies = headers.get(COOKIE);
        if (cookies != null) {
          for (String cookie : cookies) {
            builder.addHeader(COOKIE, cookie);
          }
        }
      } catch (IOException e) {
        logger.warn("Error reading cookies for URI " + resolvedUri, e);
      }

    }

    builder.setEntity(createRequestEntity(builder, event, resolvedMethod));

    return builder;
  }

  private boolean isNotIgnoredProperty(String outboundProperty) {
    return !outboundProperty.startsWith(HTTP_PREFIX) && !equalsIgnoredProperty(outboundProperty);
  }

  private boolean equalsIgnoredProperty(final String outboundProperty) {
    return CollectionUtils.exists(ignoredProperties, propertyName -> outboundProperty.equalsIgnoreCase((String) propertyName));
  }

  private HttpEntity createRequestEntity(HttpRequestBuilder requestBuilder, Event muleEvent, String resolvedMethod)
      throws MessagingException {
    HttpEntity entity;

    if (!StringUtils.isEmpty(requester.getSource()) && !(DEFAULT_PAYLOAD_EXPRESSION.equals(requester.getSource()))) {
      muleEvent = Event.builder(muleEvent).message(InternalMessage.builder(muleEvent.getMessage())
          .payload(muleContext.getExpressionManager().evaluate(requester.getSource(), muleEvent)).build()).build();
    }

    if (isEmptyBody(muleEvent, resolvedMethod)) {
      entity = new EmptyHttpEntity();
    } else {
      entity = createRequestEntityFromPayload(requestBuilder, muleEvent);
    }

    return entity;
  }

  private boolean isEmptyBody(Event event, String method) {
    HttpSendBodyMode sendBodyMode = resolveSendBodyMode(event);

    boolean emptyBody;

    if (event.getMessage().getPayload().getValue() == null && event.getMessage().getOutboundAttachmentNames().isEmpty()) {
      emptyBody = true;
    } else {
      emptyBody = DEFAULT_EMPTY_BODY_METHODS.contains(method);

      if (sendBodyMode != HttpSendBodyMode.AUTO) {
        emptyBody = (sendBodyMode == HttpSendBodyMode.NEVER);
      }
    }

    return emptyBody;
  }

  private HttpEntity createRequestEntityFromPayload(HttpRequestBuilder requestBuilder, Event muleEvent)
      throws MessagingException {
    Object payload = muleEvent.getMessage().getPayload().getValue();

    if (!muleEvent.getMessage().getOutboundAttachmentNames().isEmpty() || payload instanceof MultiPartPayload) {
      try {
        return createMultiPart(muleEvent.getMessage());
      } catch (IOException e) {
        throw new MessagingException(muleEvent, e);
      }
    }

    if (doStreaming(requestBuilder, muleEvent)) {

      if (payload instanceof InputStream) {
        return new InputStreamHttpEntity((InputStream) payload);
      } else {
        try {
          return new InputStreamHttpEntity(new ByteArrayInputStream(muleEvent.getMessageAsBytes(muleContext)));
        } catch (Exception e) {
          throw new MessagingException(muleEvent, e);
        }
      }

    } else {
      Optional<String> contentType = requestBuilder.getHeaderValue(CONTENT_TYPE);

      if (!contentType.isPresent() || contentType.get().startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())
          || contentType.get().startsWith(APPLICATION_JAVA)) {
        if (muleEvent.getMessage().getPayload().getValue() instanceof Map) {
          String body = HttpParser.encodeString(muleEvent.getMessage().getPayload().getDataType().getMediaType().getCharset()
              .orElse(getDefaultEncoding(muleContext)), (Map) payload);
          requestBuilder.addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED.toRfcString());
          return new ByteArrayHttpEntity(body.getBytes());
        }
      }

      try {
        return new ByteArrayHttpEntity(muleEvent.getMessageAsBytes(muleContext));
      } catch (Exception e) {
        throw new MessagingException(muleEvent, e);
      }
    }
  }

  protected MultipartHttpEntity createMultiPart(final InternalMessage msg) throws IOException {
    Map<String, DataHandler> attachments = Maps.newHashMap();

    for (String outboundAttachmentName : msg.getOutboundAttachmentNames()) {
      attachments.put(outboundAttachmentName, msg.getOutboundAttachment(outboundAttachmentName));
    }

    if (msg.getPayload().getValue() instanceof MultiPartPayload) {
      for (org.mule.runtime.api.message.Message part : ((MultiPartPayload) msg.getPayload().getValue()).getParts()) {
        final String partName = ((PartAttributes) part.getAttributes()).getName();
        attachments.put(partName,
                        toDataHandler(partName, part.getPayload().getValue(), part.getPayload().getDataType().getMediaType()));
      }
    }

    return new MultipartHttpEntity(HttpPartDataSource.createFrom(attachments));
  }


  private boolean doStreaming(HttpRequestBuilder requestBuilder, Event event) throws MessagingException {
    Optional<String> transferEncodingHeader = requestBuilder.getHeaderValue(TRANSFER_ENCODING);
    Optional<String> contentLengthHeader = requestBuilder.getHeaderValue(CONTENT_LENGTH);

    HttpStreamingType requestStreamingMode = resolveStreamingType(event);

    Object payload = event.getMessage().getPayload().getValue();

    if (requestStreamingMode == HttpStreamingType.AUTO) {
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
    } else if (requestStreamingMode == HttpStreamingType.ALWAYS) {
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

  private HttpStreamingType resolveStreamingType(Event event) {
    return HttpStreamingType.valueOf(requestStreamingMode.resolveStringValue(event));
  }

  private HttpSendBodyMode resolveSendBodyMode(Event event) {
    return HttpSendBodyMode.valueOf(sendBody.resolveStringValue(event));
  }
}
