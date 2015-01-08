/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

/**
 * Creates {@link org.mule.api.config.ConfigurationBuilder} instances of a given class
 */
public class MuleConfigurationBuilderFactory extends AbstractConfigurationBuilderFactory
{

    private final String className;

    public MuleConfigurationBuilderFactory(String className)
    {
        this.className = className;
    }

    @Override
    protected String getClassName()
    {
        return className;
    }
}
