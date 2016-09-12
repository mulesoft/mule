/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import static java.lang.String.valueOf;
import static org.mule.compatibility.transport.http.HttpConstants.CUSTOM_HEADER_PREFIX;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.compatibility.transport.http.CacheControlHeader;
import org.mule.compatibility.transport.http.CookieHelper;
import org.mule.compatibility.transport.http.CookieWrapper;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.HttpResponse;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseBuilder extends AbstractMessageProcessorOwner
    implements Initialisable, Processor, NonBlockingSupported {

  private static final Logger logger = LoggerFactory.getLogger(HttpResponseBuilder.class);

  private Map<String, String> headers = new HashMap<>();
  private List<CookieWrapper> cookies = new ArrayList<>();
  private String contentType;
  private String status;
  private String version;
  private CacheControlHeader cacheControl;
  private boolean propagateMuleProperties = false;
  private AbstractTransformer bodyTransformer;
  private SimpleDateFormat expiresHeaderFormatter;
  private SimpleDateFormat dateFormatter;

  private List<Processor> ownedMessageProcessor = new ArrayList<>();

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    expiresHeaderFormatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT_RFC822, Locale.US);
    expiresHeaderFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    dateFormatter = expiresHeaderFormatter;
  }

  @Override
  public Event process(Event event) throws MuleException {
    InternalMessage message = event.getMessage();

    HttpResponse httpResponse = getHttpResponse(message);

    propagateMessageProperties(httpResponse, message, event.getCorrelationId(), event.getGroupCorrelation());
    checkVersion(message);
    setStatus(httpResponse, event);
    setContentType(httpResponse, event);
    setHeaders(httpResponse, event);
    setCookies(httpResponse, event);
    setCacheControl(httpResponse, event);
    setDateHeader(httpResponse, new Date());
    setBody(httpResponse, message, event);

    return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(httpResponse).build()).build();
  }

  protected void setDateHeader(HttpResponse httpResponse, Date date) {
    httpResponse.setHeader(new Header(HttpConstants.HEADER_DATE, dateFormatter.format(date)));
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return ownedMessageProcessor;
  }

  protected void setBody(HttpResponse response, InternalMessage message, Event event) throws MuleException {
    if (bodyTransformer != null) {
      message = muleContext.getTransformationService().applyTransformers(event.getMessage(), event, bodyTransformer);
    }

    try {
      // If the payload is already HttpResponse then it already has the body set
      if (!(message.getPayload().getValue() instanceof HttpResponse)) {
        response.setBody(message, muleContext);
      }
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  private void propagateMessageProperties(HttpResponse response, InternalMessage message, String correlationId,
                                          GroupCorrelation correlation) {
    copyOutboundProperties(response, message);
    if (propagateMuleProperties) {
      copyCorrelationIdProperties(response, message, correlationId, correlation);
      copyReplyToProperty(response, message);
    }
  }

  private void copyCorrelationIdProperties(HttpResponse response, InternalMessage message, String correlationId,
                                           GroupCorrelation correlation) {
    response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MULE_CORRELATION_ID_PROPERTY, correlationId));
    if (correlation != null) {
      correlation.getGroupSize().ifPresent(s -> response
          .setHeader(new Header(CUSTOM_HEADER_PREFIX + MULE_CORRELATION_GROUP_SIZE_PROPERTY, valueOf(s))));
      correlation.getSequence()
          .ifPresent(s -> response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MULE_CORRELATION_SEQUENCE_PROPERTY, valueOf(s))));
    }
  }

  private void copyReplyToProperty(HttpResponse response, InternalMessage message) {
    if (message.getOutboundProperty(MULE_REPLY_TO_PROPERTY) != null) {
      response.setHeader(new Header(CUSTOM_HEADER_PREFIX + MULE_REPLY_TO_PROPERTY,
                                    message.getOutboundProperty(MULE_REPLY_TO_PROPERTY).toString()));
    }
  }

  protected void copyOutboundProperties(HttpResponse response, InternalMessage message) {
    for (String headerName : message.getOutboundPropertyNames()) {
      Object headerValue = message.getOutboundProperty(headerName);
      if (headerValue != null) {
        if (isMuleProperty(headerName)) {
          if (propagateMuleProperties) {
            addMuleHeader(response, headerName, headerValue);
          }
        } else if (isMultiValueCookie(headerName, headerValue)) {
          addMultiValueCookie(response, (Cookie[]) headerValue);
        } else {
          response.setHeader(new Header(headerName, headerValue.toString()));
        }
      }
    }
  }

  private void addMuleHeader(HttpResponse response, String headerName, Object headerValue) {
    response.setHeader(new Header(HttpConstants.CUSTOM_HEADER_PREFIX + headerName, headerValue.toString()));
  }

  private boolean isMuleProperty(String headerName) {
    return headerName.startsWith(MuleProperties.PROPERTY_PREFIX);
  }

  private void addMultiValueCookie(HttpResponse response, Cookie[] cookies) {
    Cookie[] arrayOfCookies = CookieHelper.asArrayOfCookies(cookies);
    for (Cookie cookie : arrayOfCookies) {
      response.addHeader(new Header(HttpConstants.HEADER_COOKIE_SET, CookieHelper.formatCookieForASetCookieHeader(cookie)));
    }
  }

  private boolean isMultiValueCookie(String headerName, Object headerValue) {
    return HttpConstants.HEADER_COOKIE_SET.equals(headerName) && headerValue instanceof Cookie[];
  }

  private HttpResponse getHttpResponse(InternalMessage message) {
    HttpResponse httpResponse;

    if (message.getPayload().getValue() instanceof HttpResponse) {
      httpResponse = (HttpResponse) message.getPayload().getValue();
    } else {
      httpResponse = new HttpResponse();
    }

    return httpResponse;
  }


  protected void setCacheControl(HttpResponse response, Event event) {
    if (cacheControl != null) {
      cacheControl.parse(event, muleContext.getExpressionLanguage());
      String cacheControlValue = cacheControl.toString();
      if (!"".equals(cacheControlValue)) {
        if (headers.get(HttpConstants.HEADER_CACHE_CONTROL) != null) {
          Header cacheControlHeader = response.getFirstHeader(HttpConstants.HEADER_CACHE_CONTROL);
          if (cacheControlHeader != null) {
            cacheControlValue += "," + cacheControlHeader.getValue();
          }
        }
        response.setHeader(new Header(HttpConstants.HEADER_CACHE_CONTROL, cacheControlValue));
      }
    }
  }

  protected void setCookies(HttpResponse response, Event event) throws MuleException {
    if (!cookies.isEmpty()) {
      for (CookieWrapper cookie : cookies) {
        try {
          cookie.parse(event, muleContext.getExpressionLanguage());
          response.addHeader(new Header(HttpConstants.HEADER_COOKIE_SET,
                                        CookieHelper.formatCookieForASetCookieHeader(cookie.createCookie())));

        } catch (Exception e) {
          throw new DefaultMuleException(e);
        }

      }
    }
  }

  protected void setHeaders(HttpResponse response, Event event) {
    if (headers != null && !headers.isEmpty()) {
      for (String headerName : headers.keySet()) {
        String name = parse(headerName, event);
        String value = headers.get(headerName);
        if (HttpConstants.HEADER_EXPIRES.equals(name)) {
          response.setHeader(new Header(name, evaluateDate(value, event)));
        } else {
          response.setHeader(new Header(name, parse(value, event)));
        }
      }
    }
  }

  protected void checkVersion(InternalMessage message) {
    version = message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
    if (version == null) {
      version = HttpConstants.HTTP11;
    }
  }

  private void setStatus(HttpResponse response, Event event) throws MuleException {
    if (status != null) {
      try {
        response.setStatusLine(HttpVersion.parse(version), Integer.valueOf(parse(status, event)));
      } catch (ProtocolException e) {
        throw new DefaultMuleException(e);
      }
    }
  }

  protected void setContentType(HttpResponse response, Event event) {
    if (contentType == null) {
      contentType = getDefaultContentType(event.getMessage());

    }
    response.setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, parse(contentType, event)));
  }

  private String parse(String value, Event event) {
    if (value != null) {
      return muleContext.getExpressionLanguage().parse(value, event, flowConstruct);
    }
    return value;
  }

  private String evaluateDate(String value, Event event) {
    Object realValue = value;

    if (value != null && muleContext.getExpressionLanguage().isExpression(value)) {
      realValue = muleContext.getExpressionLanguage().evaluate(value, event, flowConstruct);
    }

    if (realValue instanceof Date) {
      return expiresHeaderFormatter.format(realValue);
    }

    return String.valueOf(realValue);
  }



  private String getDefaultContentType(InternalMessage message) {
    final MediaType mediaType = message.getPayload().getDataType().getMediaType();
    String contentType;
    if (MediaType.ANY.matches(mediaType)) {
      contentType = HttpConstants.DEFAULT_CONTENT_TYPE;
    } else {
      contentType = mediaType.toRfcString();
    }
    return contentType;
  }


  public void setHeaders(Map<String, String> headers) {
    this.headers.putAll(headers);
  }


  public void setStatus(String status) {
    this.status = status;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setCookies(List<CookieWrapper> cookies) {
    this.cookies = cookies;
  }

  public void addHeader(String key, String value) {
    headers.put(key, value);
  }

  public void setCacheControl(CacheControlHeader cacheControl) {
    this.cacheControl = cacheControl;
  }

  public String getVersion() {
    return version;
  }

  public void setPropagateMuleProperties(boolean propagateMuleProperties) {
    this.propagateMuleProperties = propagateMuleProperties;
  }

  public void setMessageProcessor(Processor messageProcessor) {
    this.bodyTransformer = (AbstractTransformer) messageProcessor;
    ownedMessageProcessor.add(bodyTransformer);
  }


}
