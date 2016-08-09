/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.config.spring.parsers.specific.ExceptionStrategyDefinitionParser.createNoNameAttributePreProcessor;

import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.assembly.BeanAssembler;
import org.mule.runtime.config.spring.parsers.generic.ParentDefinitionParser;

import org.w3c.dom.Attr;

public class ReferenceExceptionStrategyDefinitionParser extends ParentDefinitionParser {

  public ReferenceExceptionStrategyDefinitionParser() {
    addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "exceptionListener");
    registerPreProcessor(createNoNameAttributePreProcessor());
  }

  @Override
  protected void processProperty(Attr attribute, BeanAssembler assembler) {
    if (!"http://www.mulesoft.org/schema/mule/documentation".equals(attribute.getNamespaceURI())) {
      assembler.extendBean(attribute);
    }
  }

}
