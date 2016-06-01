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
 * An {code AttributeDefinitionVisitor} is in charge of handling an attribute configuration when
 * building an object from a {@link org.mule.runtime.config.spring.dsl.model.ComponentModel}.
 *
 * Depending on the {@link org.mule.runtime.config.spring.dsl.api.AttributeDefinition} configuration
 * a method and only one method of this contract will be invoked.
 *
 * @since 4.0
 */
public interface AttributeDefinitionVisitor
{

    /**
     * Called when the attribute needs to be configured from an object provided by the Mule API.
     *
     * @param objectType the expected object type.
     */
    void onReferenceObject(Class<?> objectType);

    /**
     * Called when the attribute must be configured from another object defined in the configuration.
     *
     * @param reference the identifier of an object declared in the configuration.
     */
    void onReferenceSimpleParameter(String reference);

    /**
     * To be called when the value to be set when building the object
     * is fixed and provided by the definition of the {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition}.
     *
     * @param value the fixed value
     */
    void onFixedValue(Object value);

    /**
     * Called when the attribute is configured from a simple configuration attribute.
     *
     * @param parameterName configuration parameter name.
     * @param defaultValue default value for the configuration parameter if it has not value.
     * @param typeConverter a value converter to convert from the value provided by the config to the value required of the attribute.
     */
    void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter);

    /**
     * Called when the attribute holds all the simple configuration attributes not mapped to any other attribute.
     */
    void onUndefinedSimpleParameters();

    /**
     * Called when the attribute holds all the complex configuration attributes not mapped to any other attribute.
     */
    void onUndefinedComplexParameters();

    /**
     * Called when the attribute is configured from a list of object with a certain type.
     *
     * @param type type of the list to be set in the attribute.
     * @param identifier
     */
    void onComplexChildList(Class<?> type, Optional<String> identifier);

    /**
     * Called when the attribute is configured from an object with a certain type.
     *
     * @param type type of the attribute value.
     * @param identifier the identifier of the component
     */
    void onComplexChild(Class<?> type, Optional<String> identifier);

    /**
     * Called when the attribute is configured from the {@code ComponentModel} inner configuration.
     */
    void onValueFromTextContent();

    void onMultipleValues(AttributeDefinition[] definitions);
}
