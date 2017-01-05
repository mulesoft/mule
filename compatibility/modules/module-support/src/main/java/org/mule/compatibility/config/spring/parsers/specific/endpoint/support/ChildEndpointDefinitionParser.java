/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.parsers.specific.endpoint.support;

import org.mule.runtime.config.spring.parsers.generic.ChildEmbeddedDefinitionParser;

import org.w3c.dom.Element;

/**
 * A parser for "embedded" endpoints - ie inbound, outbound and response endpoints. Because we have automatic String ->
 * MuleEnpointURI conversion via property editors this can be used in a variety of ways. It should work directly with a simple
 * String address attribute or, in combination with a child element (handled by {@link ChildAddressDefinitionParser}, or embedded
 * in {@link AddressedEndpointDefinitionParser} for a more compact single-element approach.
 * <p>
 * This class does support references to other endpoints.
 * </p>
 * TODO - check that references are global!
 */
public class ChildEndpointDefinitionParser extends ChildEmbeddedDefinitionParser {

  public ChildEndpointDefinitionParser(Class<?> endpoint) {
    super(endpoint);
    EndpointUtils.addProperties(this);
    EndpointUtils.addPostProcess(this);
  }

  @Override
  public String getPropertyName(Element e) {
    String parent = e.getParentNode().getLocalName().toLowerCase();
    if (e.getLocalName() != null && (e.getLocalName().toLowerCase().endsWith("inbound-endpoint"))) {
      return "messageSource";
    } else if ("binding".equals(parent) || "java-interface-binding".equals(parent) || "publish-notifications".equals(parent)) {
      return "endpoint";
    } else {
      return super.getPropertyName(e);
    }
  }
}
