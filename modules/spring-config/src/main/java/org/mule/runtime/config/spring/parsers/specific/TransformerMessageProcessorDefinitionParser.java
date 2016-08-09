/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.config.spring.parsers.specific.DataTypeFactoryBean.buildDataTypeDefinition;

import org.mule.runtime.api.metadata.DataType;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Parses transformers message processors to map data type attributes to a {@link DataType}
 *
 * @since 4.0.0
 */
public class TransformerMessageProcessorDefinitionParser extends MessageProcessorWithDataTypeDefinitionParser {

  private static final String RETURN_CLASS = "returnClass";

  public TransformerMessageProcessorDefinitionParser(Class messageProcessor) {
    super(messageProcessor);
  }

  public TransformerMessageProcessorDefinitionParser() {
    super();
  }

  @Override
  protected void removeDataTypeProperties(MutablePropertyValues props) {
    props.removePropertyValue(RETURN_CLASS);
    super.removeDataTypeProperties(props);
  }

  @Override
  protected AbstractBeanDefinition parseDataType(PropertyValues sourceProperties) {
    if (sourceProperties.contains(RETURN_CLASS) || isDataTypeConfigured(sourceProperties)) {
      return buildDataTypeDefinition(getClassName(sourceProperties), sourceProperties);
    } else {
      return null;
    }
  }

  private String getClassName(PropertyValues sourceProperties) {
    return (String) (sourceProperties.contains(RETURN_CLASS) ? sourceProperties.getPropertyValue(RETURN_CLASS).getValue()
        : Object.class.getName());
  }

}
