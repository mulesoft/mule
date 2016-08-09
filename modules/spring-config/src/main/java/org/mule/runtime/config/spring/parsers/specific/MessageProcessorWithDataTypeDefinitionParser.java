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
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Gives parses support to map data type attributes to a {@link DataType}
 *
 * @since 4.0.0
 */
public class MessageProcessorWithDataTypeDefinitionParser extends MessageProcessorDefinitionParser {

  private static final String ENCODING = "encoding";
  private static final String MIME_TYPE = "mimeType";

  public MessageProcessorWithDataTypeDefinitionParser(Class messageProcessor) {
    super(messageProcessor);
  }

  public MessageProcessorWithDataTypeDefinitionParser() {
    super();
  }

  protected void removeDataTypeProperties(MutablePropertyValues props) {
    props.removePropertyValue(MIME_TYPE);
    props.removePropertyValue(ENCODING);
  }

  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    final AbstractBeanDefinition abstractBeanDefinition = super.parseInternal(element, parserContext);

    MutablePropertyValues props = abstractBeanDefinition.getPropertyValues();
    final AbstractBeanDefinition dataTypeBeanDefinition = parseDataType(props);
    if (dataTypeBeanDefinition != null) {
      props.add("returnDataType", dataTypeBeanDefinition);
      removeDataTypeProperties(props);
    }

    return abstractBeanDefinition;
  }

  protected AbstractBeanDefinition parseDataType(PropertyValues sourceProperties) {
    if (isDataTypeConfigured(sourceProperties)) {
      return buildDataTypeDefinition(Object.class.getName(), sourceProperties);
    } else {
      return null;
    }
  }

  protected boolean isDataTypeConfigured(PropertyValues sourceProperties) {
    return sourceProperties.contains(ENCODING) || sourceProperties.contains(MIME_TYPE);
  }
}
