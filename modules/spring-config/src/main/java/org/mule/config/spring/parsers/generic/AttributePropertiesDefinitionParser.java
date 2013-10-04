/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.generic;

import org.mule.config.spring.parsers.assembly.AttributeMapBeanAssemblerFactory;

public class AttributePropertiesDefinitionParser extends ChildDefinitionParser
{

    public AttributePropertiesDefinitionParser(String setterMethod)
    {
        this(setterMethod, java.lang.Object.class);
    }

    public AttributePropertiesDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
        setBeanAssemblerFactory(new AttributeMapBeanAssemblerFactory());
    }

}
