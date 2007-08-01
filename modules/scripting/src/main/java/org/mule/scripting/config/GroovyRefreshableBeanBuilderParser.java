/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.scripting.config;

import org.mule.components.script.refreshable.GroovyRefreshableBeanBuilder;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

import org.w3c.dom.Element;

public class GroovyRefreshableBeanBuilderParser extends OrphanDefinitionParser
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


