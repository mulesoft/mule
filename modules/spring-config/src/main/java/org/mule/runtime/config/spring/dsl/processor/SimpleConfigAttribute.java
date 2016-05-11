/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

/**
 * Represents a simple configuration attribute.
 *
 * @since 4.0
 */
public class SimpleConfigAttribute
{
    private String name;
    private String value;

    /**
     * @param name configuration attribute name as it appears in the configuration file.
     * @param value configuration value as defined in the configuration file.
     */
    public SimpleConfigAttribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the configuration attribute name as it appears in the configuration file.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return configuration value as defined in the configuration file.
     */
    public String getValue()
    {
        return value;
    }
}
