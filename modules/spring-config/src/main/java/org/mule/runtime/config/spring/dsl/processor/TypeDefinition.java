/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.processor;

/**
 * Defines the actual {@code Class} for the domain object to be created.
 */
public class TypeDefinition
{
    private Class<?> type;
    private String attributeName;

    private TypeDefinition()
    {
    }

    /**
     * @param type {@code Class} of the domain model to be created.
     * @return {@code TypeDefinition} created from that type.
     */
    public static TypeDefinition fromType(Class<?> type)
    {
        TypeDefinition typeDefinition = new TypeDefinition();
        typeDefinition.type = type;
        return typeDefinition;
    }

    /**
     * @param configAttributeName name of the configuration attribute that defines the domain object type.
     * @return {@code TypeDefinition} created from that type.
     */
    public static TypeDefinition fromConfigurationAttribute(String configAttributeName)
    {
        TypeDefinition typeDefinition = new TypeDefinition();
        typeDefinition.attributeName = configAttributeName;
        return typeDefinition;
    }

    public void visit(TypeDefinitionVisitor typeDefinitionVisitor)
    {
        if (type != null)
        {
            typeDefinitionVisitor.onType(type);
        }
        else
        {
            typeDefinitionVisitor.onConfigurationAttribute(attributeName);
        }
    }
}
