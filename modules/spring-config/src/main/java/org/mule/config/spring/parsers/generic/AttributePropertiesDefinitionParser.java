/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
