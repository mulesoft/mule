/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


