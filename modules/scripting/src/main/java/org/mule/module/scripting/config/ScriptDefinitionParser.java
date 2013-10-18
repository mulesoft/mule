/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.config;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.OptionalChildDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;
import org.mule.module.scripting.component.Scriptable;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ScriptDefinitionParser extends OptionalChildDefinitionParser
{
    public ScriptDefinitionParser()
    {
        super("script", Scriptable.class);
        addIgnored("name");
        addAlias("engine", "scriptEngineName");
        addAlias("file", "scriptFile");        

        // The "engine" attribute is required unless "file" is specified, in which case the 
        // file extension will be used to determine the appropriate script engine.
        String[][] requiredAttributeSets = new String[2][];
        requiredAttributeSets[0] = new String[]{"engine"};
        requiredAttributeSets[1] = new String[]{"file"};
        registerPreProcessor(new CheckRequiredAttributes(requiredAttributeSets));
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        Node node = element.getFirstChild();
        if (node != null)
        {
            String value = node.getNodeValue();
            //Support CDATA for script text
            if(node.getNextSibling()!=null && node.getNextSibling().getNodeType()== Node.CDATA_SECTION_NODE)
            {
                value = node.getNextSibling().getNodeValue();
            }

            if (!StringUtils.isBlank(value))
            {
                assembler.getBean().addPropertyValue("scriptText", value);
            }
        }
        super.postProcess(context, assembler, element);
    }
}


