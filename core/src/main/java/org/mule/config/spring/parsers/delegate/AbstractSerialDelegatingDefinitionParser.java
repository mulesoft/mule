/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows a set of definition parsers to be used, one after another, to process
 * the same element.  This lets multipe beans be generated from a single element.
 *
 * <p>Typically, subclasses will add additional processing by wrapping delegate parsers
 * with {@link AbstractPluggableDelegate}.</p>
 */
public abstract class AbstractSerialDelegatingDefinitionParser extends AbstractDelegatingDefinitionParser
{

    private int index = 0;
    private boolean first;
    private String originalId;
    private String originalName;

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        if (index == 0 || index >= size())
        {
            first = true;
            index = 0;
        }
        else
        {
            first = false;
        }
        DelegateDefinitionParser parser = get(index++);
        AbstractBeanDefinition bean = parser.parseDelegate(element, parserContext);
        if (index == size())
        {
            bean.removeAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE);
        }
        else
        {
            bean.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE, Boolean.TRUE);
        }
        return bean;
    }

    protected void addDelegate(DelegateDefinitionParser delegate)
    {
        delegate.registerPreProcessor(new PreProcessor()
        {
            public void preProcess(Element element)
            {
                if (first)
                {
                    originalId = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID);
                    originalName = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
                }
                else
                {
                    resetAttribute(element, AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID, originalId);
                    resetAttribute(element, AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME, originalName);
                }
            }
        });
        super.addDelegate(delegate);
    }

    protected void resetAttribute(Element element, String name, String value)
    {
        if (StringUtils.isEmpty(value))
        {
            if (element.hasAttribute(name))
            {
                element.removeAttribute(name);
            }
        }
        else
        {
            element.setAttribute(name, value);
        }
    }

}
