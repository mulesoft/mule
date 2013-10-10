/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
