/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.generic;

import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.core.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public class ChildEmbeddedDefinitionParser extends ChildDefinitionParser {

  public ChildEmbeddedDefinitionParser(Class<?> clazz) {
    super("messageProcessor", clazz);
    addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF);
  }

  @Override
  public BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class<?> beanClass) {
    BeanDefinitionBuilder builder = super.createBeanDefinitionBuilder(element, beanClass);
    String global = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF);
    if (StringUtils.isNotBlank(global)) {
      builder.addConstructorArgReference(global);
      builder.addDependsOn(global);
    }
    return builder;
  }

  @Override
  public String getPropertyName(Element e) {
    String parent = e.getParentNode().getLocalName().toLowerCase();
    if (e.getLocalName() != null && (e.getLocalName().toLowerCase().equals("poll"))) {
      return "messageSource";
    } else if ("wire-tap".equals(parent) || "wire-tap-router".equals(parent)) {
      return "tap";
    } else {
      return super.getPropertyName(e);
    }
  }

  @Override
  public String getBeanName(Element element) {
    if (null != element.getAttributeNode(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF)) {
      return AutoIdUtils.uniqueValue("ref:" + element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF));
    } else {
      return super.getBeanName(element);
    }
  }
}
