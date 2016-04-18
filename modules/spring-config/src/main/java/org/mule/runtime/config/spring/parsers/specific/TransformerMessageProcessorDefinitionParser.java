/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.parsers.specific;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.SimpleDataType;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 *  Parses transformers message processors to map data type attributes
 *  to a {@link DataType}
 *
 *  @since 4.0.0
 */
public class TransformerMessageProcessorDefinitionParser extends MessageProcessorDefinitionParser
{

    private static final String RETURN_CLASS = "returnClass";
    private static final String ENCODING = "encoding";
    private static final String MIME_TYPE = "mimeType";

    public TransformerMessageProcessorDefinitionParser(Class messageProcessor)
    {
        super(messageProcessor);
    }

    public TransformerMessageProcessorDefinitionParser()
    {
        super();
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        final AbstractBeanDefinition abstractBeanDefinition = super.parseInternal(element, parserContext);

        MutablePropertyValues props = abstractBeanDefinition.getPropertyValues();
        final AbstractBeanDefinition dataTypeBeanDefinition = parseDataType(props);
        if (dataTypeBeanDefinition != null)
        {
            props.add("returnDataType", dataTypeBeanDefinition);
            removeDataTypeProperties(props);
        }

        return abstractBeanDefinition;
    }

    private void removeDataTypeProperties(MutablePropertyValues props)
    {
        props.removePropertyValue(RETURN_CLASS);
        props.removePropertyValue(MIME_TYPE);
        props.removePropertyValue(ENCODING);
    }

    private AbstractBeanDefinition parseDataType(PropertyValues sourceProperties)
    {
        if (sourceProperties.contains(RETURN_CLASS) || sourceProperties.contains(ENCODING) || sourceProperties.contains(MIME_TYPE))
        {
            BeanDefinitionBuilder dataTypeBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleDataType.class);
            dataTypeBuilder.addConstructorArgValue(getClassName(sourceProperties));
            dataTypeBuilder.addConstructorArgValue(getMimeType(sourceProperties));
            dataTypeBuilder.addPropertyValue(ENCODING, getEncoding(sourceProperties));

            return dataTypeBuilder.getBeanDefinition();
        }
        else
        {
             return null;
        }
    }

    private String getEncoding(PropertyValues sourceProperties)
    {
        return sourceProperties.contains(ENCODING) ? (String) sourceProperties.getPropertyValue(ENCODING).getValue() : null;
    }

    private String getClassName(PropertyValues sourceProperties)
    {
        return (String) (sourceProperties.contains(RETURN_CLASS) ? sourceProperties.getPropertyValue(RETURN_CLASS).getValue() : Object.class.getName());
    }

    private String getMimeType(PropertyValues sourceProperties)
    {
        return (String) (sourceProperties.contains(MIME_TYPE) ? sourceProperties.getPropertyValue(MIME_TYPE).getValue() : null);
    }

}
