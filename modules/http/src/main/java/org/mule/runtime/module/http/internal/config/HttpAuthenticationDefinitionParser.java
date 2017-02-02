/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.config;

import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.module.http.internal.request.DefaultHttpAuthentication;
import org.mule.service.http.api.client.HttpAuthenticationType;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class HttpAuthenticationDefinitionParser extends ChildDefinitionParser {

  private HttpAuthenticationType httpAuthenticationType;

  public HttpAuthenticationDefinitionParser(HttpAuthenticationType httpAuthenticationType) {
    super("authentication", DefaultHttpAuthentication.class);

    this.httpAuthenticationType = httpAuthenticationType;
  }

  @Override
  protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.addConstructorArgValue(httpAuthenticationType);
    super.parseChild(element, parserContext, builder);
  }

}
