/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.w3c.dom.Element;

public class ProvideDefaultName implements PreProcessor
{
    
    private String prefix;

    public ProvideDefaultName(String prefix)
    {
        this.prefix = prefix;
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        element.setAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME,
                AutoIdUtils.getUniqueName(element, prefix));
    }

}
