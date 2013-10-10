/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.scripting.component.GroovyRefreshableBeanBuilder;

import org.w3c.dom.Element;

public class GroovyRefreshableBeanBuilderParser extends MuleOrphanDefinitionParser
{

    public GroovyRefreshableBeanBuilderParser(boolean singleton)
    {
        super(singleton);
        addIgnored("name");
    }
    
    protected Class getBeanClass(Element element)
    {
        return GroovyRefreshableBeanBuilder.class;
    }
    

}


