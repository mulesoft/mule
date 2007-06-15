/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.springframework.beans.factory.config.BeanDefinition;
import org.w3c.dom.Element;

public class NamedCompoundElementDefinitionParser extends CompoundElementDefinitionParser
{

    private String name;

    public NamedCompoundElementDefinitionParser(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    protected BeanDefinition getParentBeanDefinition(Element element)
    {
        return parserContext.getRegistry().getBeanDefinition(name);
    }

}
