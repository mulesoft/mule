/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_FACTORY_ATTRIBUTE;
import static org.mule.runtime.config.spring.util.ProcessingStrategyParserUtils.configureProcessingStrategy;

import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class FlowDefinitionParser extends OrphanDefinitionParser {

  public FlowDefinitionParser() {
    super(DefaultFlowBuilder.DefaultFlow.class, true);
    addIgnored("abstract");
    addIgnored("name");
    addIgnored(PROCESSING_STRATEGY_FACTORY_ATTRIBUTE);
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.addConstructorArgValue(element.getAttribute(ATTRIBUTE_NAME));
    builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);
    configureProcessingStrategy(element, builder, PROCESSING_STRATEGY_FACTORY_ATTRIBUTE);
    super.doParse(element, parserContext, builder);
  }
}
