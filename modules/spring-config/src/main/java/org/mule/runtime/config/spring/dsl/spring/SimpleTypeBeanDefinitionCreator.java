/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.spring.DslSimpleType.isSimpleType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.ObjectTypeVisitor;

import java.util.Optional;

import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Bean definition creator for elements that end up representing simple types.
 *
 * <p>
 * Elements that represent a simple type always have the form
 * <pre>
 *  <element value="simpleValue"/>
 * </pre>
 *
 * @since 4.0
 */
public class SimpleTypeBeanDefinitionCreator extends BeanDefinitionCreator
{

    private static final String SIMPLE_TYPE_VALUE_PARAMETER_NAME = "value";

    @Override
    boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest)
    {
        ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(createBeanDefinitionRequest.getComponentModel());
        createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeDefinition().visit(objectTypeVisitor);
        Class<?> type = objectTypeVisitor.getType();
        if (isSimpleType(type))
        {
            ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
            componentModel.setType(type);
            final String value = componentModel.getParameters().get(SIMPLE_TYPE_VALUE_PARAMETER_NAME);
            Optional<TypeConverter> typeConverterOptional = createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeConverter();
            Object convertedValue = typeConverterOptional.map(converter -> converter.convert(value)).orElse(value);
            AbstractBeanDefinition beanDefinition = genericBeanDefinition(type).addConstructorArgValue(convertedValue).getBeanDefinition();
            componentModel.setBeanDefinition(beanDefinition);
            return true;
        }
        return false;
    }

}
