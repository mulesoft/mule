/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.util;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_FACTORY_ATTRIBUTE;
import static org.mule.runtime.core.internal.util.ProcessingStrategyUtils.parseProcessingStrategy;

import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public class ProcessingStrategyParserUtils {

  public static void configureProcessingStrategy(Element element, BeanDefinitionBuilder builder) {
    configureProcessingStrategy(element, builder, PROCESSING_STRATEGY_FACTORY_ATTRIBUTE);
  }

  public static void configureProcessingStrategy(Element element, BeanDefinitionBuilder builder,
                                                 String processingStrategyProperty) {
    String processingStrategyName = element.getAttribute(PROCESSING_STRATEGY_ATTRIBUTE);
    ProcessingStrategyFactory processingStrategyFactory = parseProcessingStrategy(processingStrategyName);
    if (processingStrategyFactory != null) {
      builder.addPropertyValue(processingStrategyProperty, processingStrategyFactory);
    } else if (!StringUtils.isBlank(processingStrategyName)) {
      builder.addPropertyValue(processingStrategyProperty, new RuntimeBeanReference(processingStrategyName));
    }
  }

}
