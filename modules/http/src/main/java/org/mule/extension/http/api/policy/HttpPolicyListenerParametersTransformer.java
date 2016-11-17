/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.parseComponentIdentifier;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.listener.builder.HttpListenerErrorResponseBuilder;
import org.mule.extension.http.api.listener.builder.HttpListenerResponseBuilder;
import org.mule.extension.http.api.listener.builder.HttpListenerSuccessResponseBuilder;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.policy.PolicySourceParametersTransformer;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Implementation that does transformation from http:listener response and failure response parameters
 * to {@link Message} and viceversa.
 *
 * @since 4.0
 */
public class HttpPolicyListenerParametersTransformer implements PolicySourceParametersTransformer {

  @Override
  public boolean supports(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.equals(parseComponentIdentifier("http:listener"));
  }

  @Override
  public Message fromSuccessResponseParametersToMessage(Map<String, Object> parameters) {
    HttpListenerResponseBuilder responseBuilder = (HttpListenerResponseBuilder) parameters.get("responseBuilder");
    return responseParametersToMessage(responseBuilder);
  }

  private Message responseParametersToMessage(HttpListenerResponseBuilder responseBuilder) {
    ParameterMap headers = new ParameterMap(responseBuilder.getHeaders());
    Message.Builder messageBuilder;
    Message.PayloadBuilder builder = Message.builder();
    if (responseBuilder.getBody() == null) {
      messageBuilder = builder.nullPayload();
    } else {
      messageBuilder = builder.payload(responseBuilder.getBody());
    }
    return messageBuilder
        .attributes(new HttpResponseAttributes(responseBuilder.getStatusCode(), responseBuilder.getReasonPhrase(), headers))
        .build();
  }

  @Override
  public Message fromFailureResponseParametersToMessage(Map<String, Object> parameters) {
    HttpListenerResponseBuilder responseBuilder = (HttpListenerResponseBuilder) parameters.get("errorResponseBuilder");
    return responseParametersToMessage(responseBuilder);
  }

  @Override
  public Map<String, Object> fromMessageToSuccessResponseParameters(Message message) {
    return messageToResponseParameters(new HttpListenerSuccessResponseBuilder(), "responseBuilder", message);
  }

  @Override
  public Map<String, Object> fromMessageToErrorResponseParameters(Message message) {
    return messageToResponseParameters(new HttpListenerErrorResponseBuilder(), "errorResponseBuilder", message);
  }

  private Map<String, Object> messageToResponseParameters(HttpListenerResponseBuilder httpListenerResponseBuilder,
                                                          String responseBuilderParameterName, Message message) {
    if (message.getAttributes() instanceof HttpResponseAttributes) {
      HttpResponseAttributes httpResponseAttributes = (HttpResponseAttributes) message.getAttributes();
      httpListenerResponseBuilder.setBody(message.getPayload().getValue());
      httpListenerResponseBuilder.setHeaders(httpResponseAttributes.getHeaders());
      httpListenerResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      return ImmutableMap.<String, Object>builder().put(responseBuilderParameterName, httpListenerResponseBuilder).build();
    } else if (message.getAttributes() instanceof HttpPolicyResponseAttributes) {
      HttpPolicyResponseAttributes httpResponseAttributes = (HttpPolicyResponseAttributes) message.getAttributes();
      httpListenerResponseBuilder.setBody(message.getPayload().getValue());
      httpListenerResponseBuilder.setHeaders(httpResponseAttributes.getHeaders());
      httpListenerResponseBuilder.setStatusCode(httpResponseAttributes.getStatusCode());
      httpListenerResponseBuilder.setReasonPhrase(httpResponseAttributes.getReasonPhrase());
      return ImmutableMap.<String, Object>builder().put(responseBuilderParameterName, httpListenerResponseBuilder).build();
    } else {
      return emptyMap();
    }
  }

}
