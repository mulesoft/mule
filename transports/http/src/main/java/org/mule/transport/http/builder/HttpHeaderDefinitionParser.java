/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.builder;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractParallelDelegatingDefinitionParser;
import org.mule.module.http.internal.HttpParamType;
import org.mule.module.http.internal.HttpSingleParam;
import org.mule.module.http.internal.config.HttpMessageSingleParamDefinitionParser;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The header element may appear either inside a request-builder element of the HTTP module, a response-builder
 * element of the HTTP module, or a response-builder element of the HTTP transport. This bean definition parser delegates
 * into the corresponding parsers for each case.
 */
public class HttpHeaderDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    protected MuleDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        Node parentNode = element.getParentNode();
        String parentContext = parentNode.getLocalName();

        if (parentContext.equals("response-builder") || parentContext.equals("error-response-builder"))
        {
            String responseBuilderParent = parentNode.getParentNode().getLocalName();

            if (responseBuilderParent.equals(AbstractMuleBeanDefinitionParser.ROOT_ELEMENT) ||
                    responseBuilderParent.equals("listener"))
            {
                // header element is used in a response-builder element from the HTTP module
                return new HttpMessageSingleParamDefinitionParser(HttpSingleParam.class, HttpParamType.HEADER);
            }
            else
            {
                // header element is used in a response-builder element from the HTTP transport
                MuleDefinitionParser parser = new ChildMapEntryDefinitionParser("headers", "name", "value");
                parser.addCollection("headers");
                return parser;
            }
        }
        else if (parentContext.equals("request-builder"))
        {
            return new HttpMessageSingleParamDefinitionParser(HttpSingleParam.class, HttpParamType.HEADER);
        }
        else
        {
            throw new IllegalStateException("No parser defined for " + element.getLocalName() + " in the context " + parentContext);
        }
    }

}
