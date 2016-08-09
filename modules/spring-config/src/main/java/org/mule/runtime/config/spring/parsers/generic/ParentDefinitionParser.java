/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.generic;

import org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.runtime.config.spring.parsers.AbstractHierarchicalDefinitionParser;
import org.mule.runtime.config.spring.parsers.assembly.BeanAssembler;
import org.mule.runtime.core.util.ClassUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * Processes child property elements in Xml but sets the properties on the parent object. This is useful when an object has lots
 * of properties and it's more readable to break those properties into groups that can be represented as a sub-element in Xml.
 */
public class ParentDefinitionParser extends AbstractHierarchicalDefinitionParser {

  public ParentDefinitionParser() {
    addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_REGISTRATION);
  }

  @Override
  protected Class getBeanClass(Element element) {
    try {
      return ClassUtils.getClass(getParentBeanDefinition(element).getBeanClassName());
    } catch (Exception e) {
      // Should continue to work, but automatic collection detection etc will fail
      logger.debug("No class for " + element);
      return Object.class;
    }
  }

  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    preProcess(element);
    setParserContext(parserContext);
    setRegistry(parserContext.getRegistry());
    Class beanClass = getBeanClass(element);
    Assert.state(beanClass != null,
                 "Class returned from getBeanClass(Element) must not be null, element is: " + element.getNodeName());
    BeanDefinitionBuilder builder = createBeanDefinitionBuilder(element, beanClass);
    builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
    if (parserContext.isNested()) {
      // Inner bean definition must receive same singleton status as containing bean.
      builder.setScope(parserContext.getContainingBeanDefinition().isSingleton() ? BeanDefinition.SCOPE_SINGLETON
          : BeanDefinition.SCOPE_PROTOTYPE);
    }
    doParse(element, parserContext, builder);
    BeanAssembler beanAssembler = getBeanAssembler(element, builder);
    beanAssembler.copyBeanToTarget();
    return (AbstractBeanDefinition) beanAssembler.getTarget();
  }

  @Override
  protected void processMetadataAnnotations(Element element, String configFileIdentifier, BeanDefinitionBuilder builder) {
    // Nothing to do
    // We don't want annotations from inner elements to override the annotations from its parent element.
  }

  @Override
  protected void postProcess(ParserContext context, BeanAssembler assembler, Element element) {
    // by default the name matches the "real" bean
    if (null == element.getAttributeNode(ATTRIBUTE_NAME)) {
      element.setAttribute(ATTRIBUTE_NAME, getParentBeanName(element));
    }
    super.postProcess(context, assembler, element);
  }

}
