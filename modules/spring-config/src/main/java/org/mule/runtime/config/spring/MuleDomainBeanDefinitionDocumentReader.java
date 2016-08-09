/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.spring.parsers.specific.DomainElementsValidator;

/**
 * Allows us to hook in our own Hierarchical Parser delegate. this enables the parsing of custom spring bean elements nested
 * within each other
 *
 * @since 3.6.0
 */
public class MuleDomainBeanDefinitionDocumentReader extends MuleBeanDefinitionDocumentReader {

  public MuleDomainBeanDefinitionDocumentReader(BeanDefinitionFactory beanDefinitionFactory,
                                                XmlApplicationParser xmlApplicationParser) {
    super(beanDefinitionFactory, xmlApplicationParser);
  }

  @Override
  protected ElementValidator[] getElementsValidator() {
    return new ElementValidator[] {new DomainElementsValidator()};
  }
}
