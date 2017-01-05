/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.core.endpoint;

import static org.mule.compatibility.core.endpoint.URIBuilder.URL_ENCODER;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;

import org.mule.compatibility.core.api.endpoint.MalformedEndpointException;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds dynamic string URI from a template {@link URIBuilder}
 */
public class DynamicURIBuilder {

  protected transient final Logger logger = LoggerFactory.getLogger(DynamicURIBuilder.class);

  private final URIBuilder templateUriBuilder;

  public DynamicURIBuilder(URIBuilder templateUriBuilder) throws MalformedEndpointException {
    validateTemplate(templateUriBuilder.toString());

    this.templateUriBuilder = templateUriBuilder;
  }

  private void validateTemplate(String address) throws MalformedEndpointException {
    if (address.indexOf(":") > address.indexOf(DEFAULT_EXPRESSION_PREFIX)) {
      throw new MalformedEndpointException(TransportCoreMessages.dynamicEndpointsMustSpecifyAScheme(), address);
    }
  }

  public String build(Event event) throws URISyntaxException, UnsupportedEncodingException {
    String resolvedUri = resolveAddress(event);

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Resolved URI from template '%s' to '%s'", templateUriBuilder.getEncodedConstructor(),
                                 resolvedUri.toString()));
    }

    return resolvedUri;
  }

  private String resolveAddress(final Event event) throws URISyntaxException, UnsupportedEncodingException {
    final MuleContext muleContext = templateUriBuilder.getMuleContext();
    String resolvedAddress = templateUriBuilder.getTransformedConstructor(input -> {
      String token = (String) input;

      if (muleContext.getExpressionManager().isExpression(token)) {
        token = muleContext.getExpressionManager().parse(token, event, null);
      }

      return token;
    }, URL_ENCODER);

    return resolvedAddress;
  }

  public String getUriTemplate() {
    return templateUriBuilder.getAddress();
  }

}
