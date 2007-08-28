/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.transformers.xml.XsltTransformer;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XsltTransformerDefinitionParser extends OrphanDefinitionParser
{

    public static final String STYLESHEET = "stylesheet";
    public static final int UNDEFINED = -1;

    public XsltTransformerDefinitionParser()
    {
        super(XsltTransformer.class, false);
        addAlias("transformerFactoryClass", "xslTransformerFactory");
    }

    protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element)
    {
        super.postProcess(beanDefinition, element);
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
                getOrphanBeanAssembler(beanDefinition).extendBean("xslt", domToString(stylesheet), false);
                // block processing by Spring
                element.removeChild(stylesheet);
            }
        }
    }

    protected String domToString(Element dom)
    {
        try
        {
            // maybe change the transformer to avoid this step?
            Source source = new DOMSource(dom);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Result result = new StreamResult(output);
            TransformerFactory.newInstance().newTransformer().transform(source, result);
            return output.toString();
        }
        catch (Exception e)
        {
            throw (IllegalStateException) new IllegalStateException(e.getMessage()).initCause(e);
        }
    }

    protected void assertArgument(boolean condition, String message)
    {
        if (! condition)
        {
            throw new IllegalArgumentException(message);
        }
    }

}
