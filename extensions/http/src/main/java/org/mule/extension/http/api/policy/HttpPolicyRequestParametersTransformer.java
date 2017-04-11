/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import org.mule.extension.http.api.BaseHttpRequestAttributes;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.service.http.api.domain.ParameterMap;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Implementation that does transformation from http:request operation parameters to {@link Message} and vice versa.
 *
 * @since 4.0
 */
public class HttpPolicyRequestParametersTransformer implements OperationPolicyParametersTransformer {

  @Override
  public boolean supports(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.equals(buildFromStringRepresentation("httpn:request"));
  }

  @Override
  public Message fromParametersToMessage(Map<String, Object> parameters) {
    HttpRequesterRequestBuilder requestBuilder = (HttpRequesterRequestBuilder) parameters.get("requestBuilder");
    String path = (String) parameters.get("path");
    TypedValue<Object> body = requestBuilder.getBody();
    return Message.builder().payload(body.getValue())
        .attributes(new HttpPolicyRequestAttributes(new ParameterMap(requestBuilder.getHeaders()),
                                                    new ParameterMap(requestBuilder.getQueryParams()),
                                                    new ParameterMap(requestBuilder.getQueryParams()), path))
        .mediaType(body.getDataType().getMediaType())
        .build();
  }

  @Override
  public Map<String, Object> fromMessageToParameters(Message message) {
    if (message.getAttributes().getValue() instanceof BaseHttpRequestAttributes) {
      BaseHttpRequestAttributes requestAttributes = (BaseHttpRequestAttributes) message.getAttributes().getValue();
      HttpRequesterRequestBuilder httpRequesterRequestBuilder = new HttpRequesterRequestBuilder();
      httpRequesterRequestBuilder.setHeaders(requestAttributes.getHeaders());
      httpRequesterRequestBuilder.setQueryParams(requestAttributes.getQueryParams());
      httpRequesterRequestBuilder.setUriParams(requestAttributes.getUriParams());
      httpRequesterRequestBuilder.setBody(message.getPayload());
      return ImmutableMap.<String, Object>builder().put("requestBuilder", httpRequesterRequestBuilder).build();
    } else {
      return emptyMap();
    }
  }
}
