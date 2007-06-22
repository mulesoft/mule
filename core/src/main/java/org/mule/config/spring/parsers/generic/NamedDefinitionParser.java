/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.generic;

import org.w3c.dom.Element;

/**
 * Behaves as {@link org.mule.config.spring.parsers.generic.ParentDefinitionParser},
 * but allows any named bean to be the parent, rather than using the enclosing element in the DOM tree.
 */
public class NamedDefinitionParser extends ParentDefinitionParser
{

    private String name;

    public NamedDefinitionParser(String name)
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

    protected String getParentBeanName(Element element)
    {
        return name;
    }

}
