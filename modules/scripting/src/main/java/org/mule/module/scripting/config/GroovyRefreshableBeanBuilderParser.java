/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


