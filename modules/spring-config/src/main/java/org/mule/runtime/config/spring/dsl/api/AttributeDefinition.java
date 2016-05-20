/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api;

import static java.util.Optional.ofNullable;
import org.mule.runtime.config.spring.dsl.processor.AttributeDefinitionVisitor;

import java.util.Optional;

/**
 * Defines how to build an attribute from an object.
 * <p/>
 * An attribute may be configured to be set by using a constructor or a setter.
 * <p/>
 * The {@link org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder} allows to create an {@code AttributeDefinition}
 * from many different sources.
 *
 * @since 4.0
 */
public class AttributeDefinition
{

    private String configParameterName;
    private Object defaultValue;
    private boolean hasDefaultValue;
    private boolean undefinedSimpleParametersHolder;
    private Class<?> referenceObject;
    private Class<?> childObjectType;
    private boolean undefinedComplexParametersHolder;
    private String referenceSimpleParameter;
    private boolean collection;
    private boolean valueFromTextContent;
    private TypeConverter typeConverter;

    private AttributeDefinition()
    {
    }

    /**
     * @param visitor handler for the configuration option set for this parameter.
     */
    public void accept(AttributeDefinitionVisitor visitor)
    {
        if (configParameterName != null)
        {
            visitor.onConfigurationParameter(configParameterName, defaultValue, ofNullable(typeConverter));
        }
        else if (referenceObject != null)
        {
            visitor.onReferenceObject(referenceObject);
        }
        else if (hasDefaultValue)
        {
            visitor.onFixedValue(defaultValue);
        }
        else if (undefinedSimpleParametersHolder)
        {
            visitor.onUndefinedSimpleParameters();
        }
        else if (undefinedComplexParametersHolder)
        {
            visitor.onUndefinedComplexParameters();
        }
        else if (referenceSimpleParameter != null)
        {
            visitor.onReferenceSimpleParameter(referenceSimpleParameter);
        }
        else if (childObjectType != null && collection)
        {
            visitor.onComplexChildList(childObjectType);
        }
        else if (childObjectType != null)
        {
            visitor.onComplexChild(childObjectType);
        }
        else if (valueFromTextContent)
        {
            visitor.onValueFromTextContent();
        }
        else
        {
            throw new RuntimeException();
        }
    }

    public static class Builder
    {

        private AttributeDefinition attributeDefinition = new AttributeDefinition();

        private Builder()
        {
        }

        /**
         * @param configParameterName name of the configuration parameter from which this attribute value will be extracted.
         * @return the builder
         */
        public static Builder fromSimpleParameter(String configParameterName)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.configParameterName = configParameterName;
            return builder;
        }

        public static Builder fromSimpleParameter(String configParameterName, TypeConverter typeConverter)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.configParameterName = configParameterName;
            builder.attributeDefinition.typeConverter = typeConverter;
            return builder;
        }

        /**
         * @param defaultValue defines the default value to be used for the attribute if no other value is provided.
         * @return the builder
         */
        public Builder withDefaultValue(Object defaultValue)
        {
            attributeDefinition.hasDefaultValue = true;
            attributeDefinition.defaultValue = defaultValue;
            return this;
        }

        /**
         * @param value a fixed value which will be assigned to the attribute.
         * @return the builder
         */
        public static Builder fromFixedValue(Object value)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.hasDefaultValue = true;
            builder.attributeDefinition.defaultValue = value;
            return builder;
        }

        /**
         * Calling this method declares that the attribute will be assigned with all declared
         * simple configuration attribute and its value. By simple attribute we consider those
         * with a key and a string value as content.
         * <p/>
         * The simple attributes are store in a {@code java.util.Map} so the attribute type must
         * also be a {@code java.util.Map}.
         *
         * @return the builder
         */
        public static Builder fromUndefinedSimpleAttributes()
        {
            Builder builder = new Builder();
            builder.attributeDefinition.undefinedSimpleParametersHolder = true;
            return builder;
        }

        /**
         * Used when attribute an attribute must be set with an object provided by the
         * runtime. For instance when the object requires access to the {@code org.mule.runtime.core.api.MuleContext}
         * or a {@code org.mule.runtime.core.time.TimeSupplier}.
         *
         * @param referenceObjectType type of the object expected to be injected.
         * @return the builder
         */
        public static Builder fromReferenceObject(Class<?> referenceObjectType)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.referenceObject = referenceObjectType;
            return builder;
        }

        /**
         * Used when an attribute must be set with a complex object created from the
         * user configuration.
         *
         * @param childType type of the required complex object.
         * @return the builder
         */
        public static Builder fromChildConfiguration(Class<?> childType)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.childObjectType = childType;
            return builder;
        }

        /**
         * Calling this method declares that the attribute will be assigned with all declared
         * complex configuration object that did not were map by other {@code AttributeDefinition}s.
         * By complex attribute we consider those that are represented by complex object types.
         * <p/>
         * The complex attributes are store in a {@code java.util.List} so the attribute type must
         * also be a {@code java.util.List}.
         *
         * @return the builder
         */
        public static Builder fromUndefinedComplexAttribute()
        {
            Builder builder = new Builder();
            builder.attributeDefinition.undefinedComplexParametersHolder = true;
            return builder;
        }

        /**
         * @param referenceSimpleParameter configuration attribute that holds a reference to another
         *                                 configuration object.
         * @return the builder
         */
        public static Builder fromSimpleReferenceParameter(String referenceSimpleParameter)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.referenceSimpleParameter = referenceSimpleParameter;
            return builder;
        }

        /**
         * Used when an attribute must be set with a collection of complex objects created from the
         * user configuration.
         *
         * @param type the collection object type.
         * @return the builder
         */
        public static Builder fromChildListConfiguration(Class<?> type)
        {
            Builder builder = new Builder();
            builder.attributeDefinition.childObjectType = type;
            builder.attributeDefinition.collection = true;
            return builder;
        }

        /**
         * Used when an attribute must be created with the inner content of the configuration element.
         *
         * @return the builder
         */
        public static Builder fromTextContent()
        {
            Builder builder = new Builder();
            builder.attributeDefinition.valueFromTextContent = true;
            return builder;
        }

        /**
         * @return the {@code AttributeDefinition} created based on the defined configuration.
         */
        public AttributeDefinition build()
        {
            return attributeDefinition;
        }
    }

}
