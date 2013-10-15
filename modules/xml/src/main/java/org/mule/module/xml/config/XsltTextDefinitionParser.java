/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.xml.util.XMLUtils;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TODO
 */
public class XsltTextDefinitionParser extends ChildDefinitionParser
{
    public static final String STYLESHEET = "stylesheet";
    public static final int UNDEFINED = -1;

    /**
     * The class will be inferred from the class attribute
     *
     * @param setterMethod The target method (where the child will be injected)
     */
    public XsltTextDefinitionParser(String setterMethod)
    {
        super(setterMethod);
    }

    /**
     * @param setterMethod The target method (where the child will be injected)
     * @param clazz        The class created by this element/parser
     */
    public XsltTextDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        NodeList children = element.getChildNodes();
        if (0 != children.getLength())
        {
            Element stylesheet = null;
            for (int i = 0; i < children.getLength(); i++)
            {
                if (Node.ELEMENT_NODE == children.item(i).getNodeType())
                {
                    assertArgument(null == stylesheet, "XSLT transformer can have at most one child element");
                    stylesheet = (Element) children.item(i);
                }
            }
            if (null != stylesheet)
            {
                assertArgument(STYLESHEET.equals(stylesheet.getLocalName()),
                        "XSLT transformer child element must be named " + STYLESHEET);
                assembler.extendTarget("xslt", domToString(stylesheet), false);
                // block processing by Spring
                element.removeChild(stylesheet);
            }
        }
        super.postProcess(context, assembler, element);
    }

    @Override
    public String getPropertyName(Element e)
    {
        //We need to set this to null since we have already set the property on the parent in the postProcess() method
        return null;
    }

    protected String domToString(Element dom)
    {
        try
        {
            // maybe change the transformer to avoid this step?
            Source source = new DOMSource(dom);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Result result = new StreamResult(output);
            XMLUtils.getTransformer().transform(source, result);
            return output.toString();
        }
        catch (Exception e)
        {
            throw (IllegalStateException) new IllegalStateException(e.getMessage()).initCause(e);
        }
    }

    protected void assertArgument(boolean condition, String message)
    {
        if (!condition)
        {
            throw new IllegalArgumentException(message);
        }
    }
}
