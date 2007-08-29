/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.scripting.config;

import org.mule.config.spring.parsers.generic.MuleChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public class ScriptDefinitionParser extends MuleChildDefinitionParser
{

    public ScriptDefinitionParser(boolean singleton)
    {
        super(singleton);
        addIgnored("name");
    }

    protected Class getBeanClass(org.w3c.dom.Element element) 
    {
        return String.class;
    }
    
    protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element)
    {
        super.postProcess(beanDefinition, element);        
        beanDefinition.addConstructorArg(element.getFirstChild().getNodeValue());
    }

}


