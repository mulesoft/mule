/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_REASON_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.SET_COOKIE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.SET_COOKIE2;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.module.http.internal.multipart.HttpPartDataSource.multiPartPayloadForAttachments;
import static org.mule.runtime.module.http.internal.request.DefaultHttpRequester.DEFAULT_PAYLOAD_EXPRESSION;
import static org.mule.runtime.module.http.internal.util.HttpToMuleMessage.getMediaType;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps an HTTP response into a Mule event. A new message is set in the event with the contents of the response. The body will be
 * set as payload by default (except that the target attribute is set in the requester, in that case the enricher expression
 * provided will be used to set the response). Headers are mapped as inbound properties. The status code is mapped as an inbound
 * property {@code HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY}.
 */
public class HttpResponseToMuleEvent {

  private static final Logger logger = LoggerFactory.getLogger(HttpResponseToMuleEvent.class);

  private static final String MULTI_PART_PREFIX = "multipart/";

  private DefaultHttpRequester requester;
  private MuleContext muleContext;

  private AttributeEvaluator parseResponse;

  public HttpResponseToMuleEvent(DefaultHttpRequester requester, MuleContext muleContext, AttributeEvaluator parseResponse) {
    this.requester = requester;
    this.muleContext = muleContext;
    this.parseResponse = parseResponse;
  }

  public void convert(MuleEvent muleEvent, HttpResponse response, String uri) throws MessagingException {
    String responseContentType = response.getHeaderValueIgnoreCase(CONTENT_TYPE);
    DataType dataType = muleEvent.getMessage().getDataType();
    if (StringUtils.isEmpty(responseContentType) && !MediaType.ANY.matches(dataType.getMediaType())) {
      responseContentType = dataType.getMediaType().toRfcString();
    }

    InputStream responseInputStream = ((InputStreamHttpEntity) response.getEntity()).getInputStream();
    Charset encoding = getMediaType(responseContentType, getDefaultEncoding(muleContext)).getCharset().get();

    Map<String, Serializable> inboundProperties = getInboundProperties(response);
    Object payload = responseInputStream;

    if (responseContentType != null && parseResponse.resolveBooleanValue(muleEvent)) {
      if (responseContentType.startsWith(MULTI_PART_PREFIX)) {
        try {
          payload = multiPartPayloadForAttachments(responseContentType, responseInputStream);
        } catch (IOException e) {
          throw new MessagingException(muleEvent, e);
        }
      } else if (responseContentType.startsWith(APPLICATION_X_WWW_FORM_URLENCODED.toRfcString())) {
        payload = HttpParser.decodeString(IOUtils.toString(responseInputStream), encoding);
      }
    }

    final Builder builder =
        MuleMessage.builder().payload(muleEvent.getMessage().getPayload()).inboundProperties(inboundProperties);

    if (StringUtils.isEmpty(responseContentType)) {
      builder.mediaType(muleEvent.getMessage().getDataType().getMediaType());
    } else {
      builder.mediaType(MediaType.parse(responseContentType));
    }

    MuleMessage message = builder.build();

    muleEvent.setMessage(message);
    setResponsePayload(payload, muleEvent);

    if (requester.getConfig().isEnableCookies()) {
      processCookies(response, uri);
    }
  }

  private Map<String, Serializable> getInboundProperties(HttpResponse response) {
    Map<String, Serializable> properties = new HashMap<>();

    for (String headerName : response.getHeaderNames()) {
      // Content-Type was already processed
      if (!CONTENT_TYPE.equalsIgnoreCase(headerName)) {
        properties.put(headerName, getHeaderValueToProperty(response, headerName));
      }
    }

    properties.put(HTTP_STATUS_PROPERTY, response.getStatusCode());
    properties.put(HTTP_REASON_PROPERTY, response.getReasonPhrase());

    return properties;
  }

  private Serializable getHeaderValueToProperty(HttpResponse response, String headerName) {
    Collection<String> headerValues = response.getHeaderValues(headerName);
    if (headerValues.size() > 1) {
      return new ArrayList<>(headerValues);
    }
    return response.getHeaderValue(headerName);
  }

  /**
   * Stores the response payload (body of the HTTP response) in the Mule message according to the "target" property. If empty, it
   * will be stored in the payload. If not, it will use the target expression to enrich the message with the body of the response.
   */
  private void setResponsePayload(Object payload, MuleEvent muleEvent) {
    if (StringUtils.isEmpty(requester.getTarget()) || DEFAULT_PAYLOAD_EXPRESSION.equals(requester.getTarget())) {
      muleEvent.setMessage(MuleMessage.builder(muleEvent.getMessage()).payload(payload)
          .mediaType(muleEvent.getMessage().getDataType().getMediaType()).build());
    } else {
      muleContext.getExpressionManager().enrich(requester.getTarget(), muleEvent, null, payload);
    }
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
      requester.getConfig().getCookieManager().put(URI.create(uri), cookieHeaders);
    } catch (IOException e) {
      logger.warn("Error storing cookies for URI " + uri, e);
    }
  }
}
