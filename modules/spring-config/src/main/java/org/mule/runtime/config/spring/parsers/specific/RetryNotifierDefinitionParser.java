/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;

import org.mule.runtime.config.spring.parsers.generic.OptionalChildDefinitionParser;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class RetryNotifierDefinitionParser extends OptionalChildDefinitionParser {

  public RetryNotifierDefinitionParser() {
    super("notifier");
  }

  public RetryNotifierDefinitionParser(Class clazz) {
    super("notifier", clazz);
  }


  @Override
  protected boolean isChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    return !isConfigElement(element);
  }

  private boolean isConfigElement(Element element) {
    return getAncestorBeanName(element).equals(OBJECT_MULE_CONFIGURATION);
  }

  private String getAncestorBeanName(Element element) {
    return ((Element) ((Element) element.getParentNode()).getParentNode()).getAttribute(ATTRIBUTE_NAME);
  }
}
