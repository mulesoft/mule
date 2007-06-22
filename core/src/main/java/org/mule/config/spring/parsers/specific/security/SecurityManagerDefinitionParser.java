/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.security;

import org.mule.config.MuleProperties;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class SecurityManagerDefinitionParser extends NamedDefinitionParser
{

    public SecurityManagerDefinitionParser()
    {
        super(MuleProperties.OBJECT_SECURITY_MANAGER);
        addIgnored("id");
        addIgnored("name");
        addIgnored("type");
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        return super.parseInternal(element, parserContext);
    }

}
