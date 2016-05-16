/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;

import java.lang.reflect.Field;

import org.w3c.dom.Element;

/**
 * Contains the required information for parsing an {@link Element} to a {@link ParameterModel Parameter}
 * or {@link Field} declaration
 *
 * @since 4.0
 */
class ParameterParsingDescriptor
{
    private final ElementDescriptor element;
    private final String fieldName;
    private final String childElementName;
    private final MetadataType metadataType;
    private final Object defaultValue;

    /**
     * Creates a new instance which contains the information required to parse
     * an {@link Element} to a {@link Field}
     *
     * @param rootElement          the {@link ElementDescriptor} of the {@link Element}
     * @param fieldName          the name of the {@link Field} tha will be populated with this element's value
     * @param childElementName   the name of the child {@link Element} that has to be parsed
     * @param metadataType    the {@link MetadataType} of the {@link Field}
     * @param defaultValue    the {@code defaultValue} of the {@link Field}
     */
    ParameterParsingDescriptor(ElementDescriptor rootElement, String fieldName, String childElementName, MetadataType metadataType, Object defaultValue)
    {
        this.element = rootElement;
        this.fieldName = fieldName;
        this.childElementName = childElementName;
        this.metadataType = metadataType;
        this.defaultValue = defaultValue;
    }

    public ElementDescriptor getElement()
    {
        return element;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public String getChildElementName()
    {
        return childElementName;
    }

    public MetadataType getMetadataType()
    {
        return metadataType;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }
}
