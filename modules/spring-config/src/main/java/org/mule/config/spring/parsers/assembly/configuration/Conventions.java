/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly.configuration;

/**
 * Overloads the Spring {@link org.springframework.core.Conventions} class, specifically the {@link #attributeNameToPropertyName(String)}
 * Method to evaluate the first character of the attribute name and ignore if it is upper case since this is not valid Bean notation
 * and Mule uses upper case to signify a non-bean attribute name.
 */
public final class Conventions
{

    private Conventions()
    {
        // do not instantiate
    }

    /**
     * Overloads the Spring version of this method to tak into account the first character in the attribute name
     * An upper case char as the first letter of a bean name is not allowed. In Mule this also signifies a non bean property
     * @param attributeName the attribute name to parse
     * @return the correctly formatted bean name
     */
    public static String attributeNameToPropertyName(String attributeName)
    {
        char[] chars = attributeName.toCharArray();
        if(Character.isUpperCase(chars[0]))
        {
            return attributeName;
        }
        else
        {
            return org.springframework.core.Conventions.attributeNameToPropertyName(attributeName);
        }
    }
}
