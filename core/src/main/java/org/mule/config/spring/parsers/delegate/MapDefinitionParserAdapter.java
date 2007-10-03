/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.assembly.MapBeanAssemblerFactory;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class MapDefinitionParserAdapter extends AbstractDelegatingDefinitionParser
{

    public MapDefinitionParserAdapter(ChildDefinitionParser delegate)
    {
        super(new MuleDefinitionParser[]{delegate});
        delegate.setBeanAssemblerFactory(new MapBeanAssemblerFactory());
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        return getDelegate(0).parseDelegate(element, parserContext);
    }

}
