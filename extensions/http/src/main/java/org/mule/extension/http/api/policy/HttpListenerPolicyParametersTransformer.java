/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.listener.builder.HttpListenerResponseBuilder;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.service.http.api.domain.ParameterMap;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Implementation that does transformation from http:listener response and failure response parameters to {@link Message} and vice
 * versa.
 *
 * @since 4.0
 */
public class HttpListenerPolicyParametersTransformer implements SourcePolicyParametersTransformer {

  @Override
  public boolean supports(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.equals(buildFromStringRepresentation("httpn:listener"));
  }

  @Override
  public Message fromSuccessResponseParametersToMessage(Map<String, Object> parameters) {
    HttpListenerResponseBuilder responseBuilder = (HttpListenerResponseBuilder) parameters.get("response");
    return responseParametersToMessage(responseBuilder, 200);
  }

  private Message responseParametersToMessage(HttpListenerResponseBuilder responseBuilder, int defaultStatusCode) {
    ParameterMap headers = new ParameterMap(responseBuilder.getHeaders());
    Message.Builder messageBuilder;
    Message.PayloadBuilder builder = Message.builder();
    TypedValue<Object> body = responseBuilder.getBody();
    if (body.getValue() == null) {
      messageBuilder = builder.nullPayload();
    } else {
      messageBuilder = builder.payload(body.getValue()).mediaType(body.getDataType().getMediaType());
    }
    int statusCode = responseBuilder.getStatusCode() == null ? defaultStatusCode : responseBuilder.getStatusCode();
    return messageBuilder
        .attributes(new HttpResponseAttributes(statusCode, responseBuilder.getReasonPhrase(), headers))
        .build();
  }

  @Override
  public Message fromFailureResponseParametersToMessage(Map<String, Object> parameters) {
    HttpListenerResponseBuilder responseBuilder = (HttpListenerResponseBuilder) parameters.get("errorResponse");
    return responseParametersToMessage(responseBuilder, 500);
  }

  @Override
  public Map<String, Object> fromMessageToSuccessResponseParameters(Message message) {
    return messageToResponseParameters(new HttpListenerResponseBuilder(), "response", message);
  }

  @Override
  public Map<String, Object> fromMessageToErrorResponseParameters(Message message) {
    return messageToResponseParameters(new HttpListenerResponseBuilder(), "errorResponse", message);
  }

  private Map<String, Object> messageToResponseParameters(HttpListenerResponseBuilder httpListenerResponseBuilder,
                                                          String responseBuilderParameterName, Message message) {
    ImmutableMap.Builder<String, Object> mapBuilder =
        ImmutableMap.<String, Object>builder().put(responseBuilderParameterName, httpListenerResponseBuilder);
    if (message.getAttributes().getValue() instanceof HttpResponseAttributes) {
      HttpResponseAttributes httpResponseAttributes = (HttpResponseAttributes) message.getAttributes().getValue();
      httpListenerResponseBuilder.setBody(message.getPayload());
      httpListenerResponseBuilder.setHeaders(httpResponseAttributes.getHeaders());
      httpListenerResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      return mapBuilder.build();
    } else if (message.getAttributes().getValue() instanceof HttpPolicyResponseAttributes) {
      HttpPolicyResponseAttributes httpResponseAttributes = (HttpPolicyResponseAttributes) message.getAttributes().getValue();
      httpListenerResponseBuilder.setBody(message.getPayload());
      httpListenerResponseBuilder.setHeaders(httpResponseAttributes.getHeaders());
      httpListenerResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      return mapBuilder.build();
    } else {
      return mapBuilder.build();
    }
  }

}
