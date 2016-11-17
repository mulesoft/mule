/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.dsl.api.component.ComponentIdentifier.parseComponentIdentifier;
import org.mule.extension.http.api.BaseHttpRequestAttributes;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.policy.PolicyOperationParametersTransformer;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation that does transformation from http:request operation parameters
 * to {@link Message} and viceversa.
 *
 * @since 4.0
 */
public class HttpPolicyRequestParametersTransformer implements PolicyOperationParametersTransformer {

  @Override
  public boolean supports(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.equals(parseComponentIdentifier("http:request"));
  }

  @Override
  public Message fromParametersToMessage(Map<String, Object> parameters) {
    HttpRequesterRequestBuilder requestBuilder = (HttpRequesterRequestBuilder) parameters.get("requestBuilder");
    String path = (String) parameters.get("path");
    // TODO support body once MULE-10711 gets done
    return Message.builder().payload("empty body")
        .attributes(new HttpPolicyRequestAttributes(new ParameterMap(requestBuilder.getHeaders()),
                                                    new ParameterMap(requestBuilder.getQueryParams()),
                                                    new ParameterMap(requestBuilder.getQueryParams()), path))
        .build();
  }

  @Override
  public Map<String, Object> fromMessageToParameters(Message message) {
    if (message.getAttributes() instanceof BaseHttpRequestAttributes) {
      BaseHttpRequestAttributes requestAttributes = (BaseHttpRequestAttributes) message.getAttributes();
      HttpRequesterRequestBuilder httpRequesterRequestBuilder = new HttpRequesterRequestBuilder();
      httpRequesterRequestBuilder.setHeaders(requestAttributes.getHeaders());
      httpRequesterRequestBuilder.setQueryParams(requestAttributes.getQueryParams());
      httpRequesterRequestBuilder.setUriParams(requestAttributes.getUriParams());
      return ImmutableMap.<String, Object>builder().put("requestBuilder", httpRequesterRequestBuilder).build();
    } else {
      return emptyMap();
    }
  }
}
