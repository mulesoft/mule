/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.config;

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributesWhenNoChildren;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;

import org.w3c.dom.Element;

public class WsSecurityDefinitionParser extends ParentContextDefinitionParser
{
    
    public WsSecurityDefinitionParser(Class wsSecurityClass)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, createRootDefinitionParser(wsSecurityClass));
        otherwise(createChildDefinitionParser(wsSecurityClass));

        super.registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{"ref"},
                                                                           new String[]{"mule-security-manager", "ws-config", "ws-custom-validator"}));

    }
    
    public static MuleOrphanDefinitionParser createRootDefinitionParser(Class wsSecurityClass)
    {
        return new MuleOrphanDefinitionParser(wsSecurityClass, true);
    }
    
    public static ChildDefinitionParser createChildDefinitionParser(Class wsSecurityClass)
    {
        ChildDefinitionParser childParser = new ChildDefinitionParser("wsSecurity", wsSecurityClass, false);
        childParser.registerPreProcessor(createNoNameAttributePreProcessor());
        return childParser;
    }

    static PreProcessor createNoNameAttributePreProcessor()
    {
        return new PreProcessor()
        {
            @Override
            public void preProcess(PropertyConfiguration config, Element element)
            {
                if (element.hasAttribute("name"))
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("name attribute on exception strategy is only allowed on global exception strategies"));
                }
            }
        };
    }


}
