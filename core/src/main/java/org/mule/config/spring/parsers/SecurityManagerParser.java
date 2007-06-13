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

import org.mule.config.MuleProperties;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class SecurityManagerParser extends AbstractBeanDefinitionParser
{

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        return getSecuritymanager(element, parserContext);
    }

    public static AbstractBeanDefinition getSecuritymanager(Element element, ParserContext parserContext)
    {
        // this is a hack copied from other BDPs that stops Spring from complaining that "id"
        // is not defined
        element.setAttribute("id", element.getAttribute("name"));
        // we pull the "real" bean from spring - it is created in default-mule-config.xml
        return (AbstractBeanDefinition)parserContext.getRegistry().getBeanDefinition(MuleProperties.OBJECT_SECURITY_MANAGER);
    }

}
