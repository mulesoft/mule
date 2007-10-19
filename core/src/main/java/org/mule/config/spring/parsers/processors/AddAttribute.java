/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.processors;

import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;

import org.w3c.dom.Element;

public class AddAttribute implements PreProcessor, PostProcessor
{

    private String name;
    private String value;

    public AddAttribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    public void postProcess(BeanAssembler assembler, Element element)
    {
        element.setAttribute(name, value);
    }

    public void preProcess(PropertyConfiguration config, Element element)
    {
        element.setAttribute(name, value);
    }

}
