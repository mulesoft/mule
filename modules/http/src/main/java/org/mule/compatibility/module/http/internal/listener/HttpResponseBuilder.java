/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.http.internal.listener;

import static java.lang.String.format;
import static org.mule.compatibility.module.http.api.HttpConstants.RequestProperties.HTTP_PREFIX;
import static org.mule.compatibility.module.http.api.HttpConstants.RequestProperties.HTTP_STATUS_PROPERTY;
import static org.mule.compatibility.module.http.api.HttpConstants.RequestProperties.HTTP_VERSION_PROPERTY;
import static org.mule.compatibility.module.http.api.HttpConstants.ResponseProperties.HTTP_REASON_PROPERTY;
import static org.mule.compatibility.module.http.api.requester.HttpStreamingType.ALWAYS;
import static org.mule.compatibility.module.http.api.requester.HttpStreamingType.AUTO;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import static org.mule.runtime.core.util.UUID.getUUID;
import static org.mule.service.http.api.HttpConstants.HttpStatus.getReasonPhraseForStatusCode;
import static org.mule.service.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.service.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.service.http.api.HttpHeaders.Values.MULTIPART_FORM_DATA;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.encodeString;

import org.mule.compatibility.module.http.api.requester.HttpStreamingType;
import org.mule.compatibility.module.http.internal.HttpMessageBuilder;
import org.mule.compatibility.module.http.internal.HttpParamType;
import org.mule.compatibility.module.http.internal.multipart.HttpMultipartEncoder;
import org.mule.compatibility.module.http.internal.multipart.HttpPartDataSource;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.NumberUtils;
import org.mule.service.http.api.HttpHeaders;
import org.mule.service.http.api.domain.HttpProtocol;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseBuilder extends HttpMessageBuilder implements Initialisable, MuleContextAware {

  public static final String MULTIPART = "multipart";
  private Logger logger = LoggerFactory.getLogger(getClass());
  private String statusCode;
  private String reasonPhrase;
  private boolean disablePropertiesAsHeaders = false;
  private HttpStreamingType responseStreaming = AUTO;
  private boolean multipartEntityWithNoMultipartContentyTypeWarned;
  private boolean mapPayloadButNoUrlEncodedContentyTypeWarned;
  private AttributeEvaluator statusCodeEvaluator;
  private AttributeEvaluator reasonPhraseEvaluator;
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    init();
  }

  void init() throws InitialisationException {
    statusCodeEvaluator = new AttributeEvaluator(statusCode).initialize(muleContext.getExpressionManager());
    reasonPhraseEvaluator = new AttributeEvaluator(reasonPhrase).initialize(muleContext.getExpressionManager());
  }

  public HttpResponse build(org.mule.service.http.api.domain.message.response.HttpResponseBuilder httpResponseBuilder,
                            Event event)
      throws MessagingException {
    final HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();
    final Set<String> outboundProperties = event.getMessage().getOutboundPropertyNames();

    if (!disablePropertiesAsHeaders) {
      for (String outboundPropertyName : outboundProperties) {
        if (isNotIgnoredProperty(outboundPropertyName)) {
          final Object outboundPropertyValue = event.getMessage().getOutboundProperty(outboundPropertyName);
          httpResponseHeaderBuilder.addHeader(outboundPropertyName, outboundPropertyValue);
        }
      }
    }

    DataType dataType = event.getMessage().getPayload().getDataType();
    if (!MediaType.ANY.matches(dataType.getMediaType())) {
      httpResponseHeaderBuilder.addHeader(CONTENT_TYPE, dataType.getMediaType().toRfcString());
    }

    ParameterMap resolvedHeaders = resolveParams(event, HttpParamType.HEADER, muleContext);
    for (String name : resolvedHeaders.keySet()) {
      final Collection<String> paramValues = resolvedHeaders.getAll(name);
      for (String value : paramValues) {
        if (TRANSFER_ENCODING.equals(name) && !supportsTransferEncoding(event)) {
          logger
              .debug("Client HTTP version is lower than 1.1 so the unsupported 'Transfer-Encoding' header has been removed and 'Content-Length' will be sent instead.");
        } else {
          httpResponseHeaderBuilder.addHeader(name, value);
        }
      }
    }

    final String configuredContentType = httpResponseHeaderBuilder.getContentType();
    final String existingTransferEncoding = httpResponseHeaderBuilder.getTransferEncoding();
    final String existingContentLength = httpResponseHeaderBuilder.getContentLength();

    HttpEntity httpEntity;

    if (!event.getMessage().getOutboundAttachmentNames().isEmpty()
        || event.getMessage().getPayload().getValue() instanceof MultiPartPayload) {
      if (configuredContentType == null) {
        httpResponseHeaderBuilder.addContentType(createMultipartFormDataContentType());
      } else if (!configuredContentType.startsWith(MULTIPART)) {
        warnNoMultipartContentTypeButMultipartEntity(httpResponseHeaderBuilder.getContentType());
      }
      httpEntity = createMultipartEntity(event, httpResponseHeaderBuilder.getContentType());
      resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding(event),
                      (ByteArrayHttpEntity) httpEntity);
    } else {
      final Object payload = event.getMessage().getPayload().getValue();
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

        if (responseStreaming == ALWAYS && supportsTransferEncoding(event)) {
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
          if (supportsTransferEncoding(event)) {
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
          ByteArrayHttpEntity byteArrayHttpEntity = new ByteArrayHttpEntity(event.getMessageAsBytes(muleContext));
          resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength,
                          supportsTransferEncoding(event), byteArrayHttpEntity);
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
        httpResponseBuilder.addHeader(headerName, value);
      }
    }

    Integer resolvedStatusCode = resolveStatusCode(event);
    if (resolvedStatusCode != null) {
      httpResponseBuilder.setStatusCode(resolvedStatusCode);
    }
    String resolvedReasonPhrase = resolveReasonPhrase(event, resolvedStatusCode);
    if (resolvedReasonPhrase != null) {
      httpResponseBuilder.setReasonPhrase(resolvedReasonPhrase);
    }
    httpResponseBuilder.setEntity(httpEntity);
    return httpResponseBuilder.build();
  }

  private boolean supportsTransferEncoding(Event event) {
    String httpVersion = event.getMessage().<String>getInboundProperty(HTTP_VERSION_PROPERTY);
    return !(HttpProtocol.HTTP_0_9.asString().equals(httpVersion) || HttpProtocol.HTTP_1_0.asString().equals(httpVersion));
  }

  private void resolveEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder, String existingTransferEncoding,
                               String existingContentLength, boolean supportsTranferEncoding,
                               ByteArrayHttpEntity byteArrayHttpEntity) {
    if (responseStreaming == ALWAYS
        || (responseStreaming == AUTO && existingContentLength == null && CHUNKED.equals(existingTransferEncoding))) {
      if (supportsTranferEncoding) {
        setupChunkedEncoding(httpResponseHeaderBuilder);
      }
    } else {
      setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
    }
  }

  private boolean isNotIgnoredProperty(String outboundPropertyName) {
    return !outboundPropertyName.startsWith(HTTP_PREFIX) && !outboundPropertyName.equalsIgnoreCase(CONNECTION)
        && !outboundPropertyName.equalsIgnoreCase(TRANSFER_ENCODING);
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

  private Integer resolveStatusCode(Event event) {
    if (statusCode != null) {
      return statusCodeEvaluator.resolveIntegerValue(event);
    }

    Object statusCodeOutboundProperty = event.getMessage().getOutboundProperty(HTTP_STATUS_PROPERTY);
    if (statusCodeOutboundProperty != null) {
      return NumberUtils.toInt(statusCodeOutboundProperty);
    }

    return null;
  }

  private String resolveReasonPhrase(Event event, Integer resolvedStatusCode) {
    if (reasonPhrase != null) {
      return reasonPhraseEvaluator.resolveStringValue(event);
    }

    Object reasonPhraseOutboundProperty = event.getMessage().getOutboundProperty(HTTP_REASON_PROPERTY);
    if (reasonPhraseOutboundProperty != null) {
      return reasonPhraseOutboundProperty.toString();
    } else if (resolvedStatusCode != null) {
      return getReasonPhraseForStatusCode(resolvedStatusCode);
    }

    return null;
  }

  private String createMultipartFormDataContentType() {
    return format("%s; boundary=%s", MULTIPART_FORM_DATA, getUUID());
  }

  private HttpEntity createUrlEncodedEntity(Event event, Map payload) {
    final Map mapPayload = payload;
    HttpEntity entity = new EmptyHttpEntity();
    if (!mapPayload.isEmpty()) {
      String encodedBody;
      final Charset encoding = event.getMessage().getPayload().getDataType().getMediaType().getCharset().get();
      if (mapPayload instanceof ParameterMap) {
        encodedBody = encodeString(((ParameterMap) mapPayload).toListValuesMap(), encoding);
      } else {
        encodedBody = encodeString(mapPayload, encoding);
      }
      entity = new ByteArrayHttpEntity(encodedBody.getBytes());
    }
    return entity;
  }

  private void warnMapPayloadButNoUrlEncodedContentType(String contentType) {
    if (!mapPayloadButNoUrlEncodedContentyTypeWarned) {
      logger
          .warn(format("Payload is a Map which will be used to generate an url encoded http body but Contenty-Type specified is %s and not %s",
                       contentType, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED));
      mapPayloadButNoUrlEncodedContentyTypeWarned = true;
    }
  }

  private void warnNoMultipartContentTypeButMultipartEntity(String contentType) {
    if (!multipartEntityWithNoMultipartContentyTypeWarned) {
      logger
          .warn(format("Sending http response with Content-Type %s but the message has attachment and a multipart entity is generated",
                       contentType));
      multipartEntityWithNoMultipartContentyTypeWarned = true;
    }
  }

  private HttpEntity createMultipartEntity(Event event, String contentType) throws MessagingException {
    if (logger.isDebugEnabled()) {
      logger.debug("Message contains outbound attachments. Ignoring payload and trying to generate multipart response");
    }
    final HashMap<String, DataHandler> parts = new HashMap<>();
    for (String outboundAttachmentName : event.getMessage().getOutboundAttachmentNames()) {
      parts.put(outboundAttachmentName, event.getMessage().getOutboundAttachment(outboundAttachmentName));
    }

    try {
      if (event.getMessage().getPayload().getValue() instanceof MultiPartPayload) {
        for (org.mule.runtime.api.message.Message part : ((MultiPartPayload) event.getMessage().getPayload().getValue())
            .getParts()) {
          final String partName = ((PartAttributes) part.getAttributes()).getName();
          parts.put(partName,
                    toDataHandler(partName, part.getPayload().getValue(), part.getPayload().getDataType().getMediaType()));
        }
      }

      final MultipartHttpEntity multipartEntity = new MultipartHttpEntity(HttpPartDataSource.createFrom(parts));
      return new ByteArrayHttpEntity(HttpMultipartEncoder.createMultipartContent(multipartEntity, contentType));
    } catch (Exception e) {
      throw new MessagingException(event, e);
    }
  }

  public static HttpResponseBuilder emptyInstance(MuleContext muleContext) throws InitialisationException {
    final HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder();
    httpResponseBuilder.setMuleContext(muleContext);
    httpResponseBuilder.init();
    return httpResponseBuilder;
  }

  public void setReasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public void setDisablePropertiesAsHeaders(boolean disablePropertiesAsHeaders) {
    this.disablePropertiesAsHeaders = disablePropertiesAsHeaders;
  }


  public void setResponseStreaming(HttpStreamingType responseStreaming) {
    this.responseStreaming = responseStreaming;
  }

  public HttpStreamingType getResponseStreaming() {
    return responseStreaming;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }
}
