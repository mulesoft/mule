/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;

import java.util.Optional;

/**
 * Abstract {@link AttributeDefinitionVisitor} so clients do not have to implement every method.
 *
 * @since 4.0
 */
public class AbstractAttributeDefinitionVisitor implements AttributeDefinitionVisitor
{

    @Override
    public void onReferenceObject(Class<?> objectType)
    {
    }

    @Override
    public void onReferenceSimpleParameter(String reference)
    {
    }

    @Override
    public void onFixedValue(Object value)
    {
    }

    @Override
    public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter)
    {
    }

    @Override
    public void onUndefinedSimpleParameters()
    {
    }

    @Override
    public void onUndefinedComplexParameters()
    {
    }

    @Override
    public void onComplexChildList(Class<?> type, Optional<String> identifierOptional)
    {
    }

    @Override
    public void onComplexChild(Class<?> type, Optional<String> identifierOptional)
    {
    }

    @Override
    public void onValueFromTextContent()
    {
    }

    @Override
    public void onMultipleValues(AttributeDefinition[] definitions)
    {
    }
}
