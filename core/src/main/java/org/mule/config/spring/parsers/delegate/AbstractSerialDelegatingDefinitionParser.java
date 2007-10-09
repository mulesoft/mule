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
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.PropertyConfiguration;
import org.mule.util.StringUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows a set of definition parsers to be used, one after another, to process
 * the same element.  This lets multiple beans be generated from a single element.
 *
 * <p>Since each bean typically needs a spearate name, this class guarantees that the
 * name and id attributes are reset before each call.  Delegates can then modify these
 * on the element without worrying about interfering with other parsers.</p>
 *
 * <p>Typically, subclasses will add additional processing with
 * {@link org.mule.config.spring.parsers.PreProcessor} and
 * {@link org.mule.config.spring.parsers.PostProcessor} anonymous classes.</p>
 */
public abstract class AbstractSerialDelegatingDefinitionParser extends AbstractDelegatingDefinitionParser
{

    private int index = 0;
    private boolean first;
    private String originalId;
    private String originalName;
    private Set handledExceptions = new HashSet();

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
        AbstractBeanDefinition bean = null;
        while (null == bean && index < size())
        {
            try
            {
                MuleDefinitionParser parser = getDelegate(index);
                bean = doSingleBean(index++, parser, element, parserContext);
            }
            catch (RuntimeException e)
            {
                if (handledExceptions.contains(e.getClass()))
                {
                    bean = null;
                }
                else
                {
                    throw e;
                }
            }
        }
        if (null != bean)
        {
            if (index == size())
            {
                bean.removeAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE);
            }
            else
            {
                bean.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE, Boolean.TRUE);
            }
        }
        return bean;
    }

    protected AbstractBeanDefinition doSingleBean(int index, MuleDefinitionParser parser,
                                                  Element element, ParserContext parserContext)
    {
        return parser.parseDelegate(element, parserContext);
    }

    protected MuleDefinitionParser addDelegate(MuleDefinitionParser delegate)
    {
        delegate.registerPreProcessor(new PreProcessor()
        {
            public void preProcess(PropertyConfiguration config, Element element)
            {
                if (first)
                {
                    originalId = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID);
                    originalName = element.getAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
                }
                else
                {
                    resetNameAndId(element);
                }
            }
        });
        return super.addDelegate(delegate);
    }

    protected void resetNameAndId(Element element)
    {
        resetAttribute(element, AbstractMuleBeanDefinitionParser.ATTRIBUTE_ID, originalId);
        resetAttribute(element, AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME, originalName);
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

    protected void addHandledException(Class exception)
    {
        handledExceptions.add(exception);
    }

    /**
     * A utility class for selecting certain attributes.  If the attributes are enabled,
     * the default is set to block others; if specific attributes are disabled the default
     * is set to allow others.
     *
     * @param delegate
     * @param attributes
     * @param enable
     */
    public static void enableAttributes(MuleDefinitionParser delegate, String[] attributes, boolean enable)
    {
        // if enabling specific attributes, block globally
        delegate.setIgnoredDefault(enable);

        Iterator names = Arrays.asList(attributes).iterator();
        while (names.hasNext())
        {
            String name = (String) names.next();
            if (enable)
            {
                delegate.removeIgnored(name);
            }
            else
            {
                delegate.addIgnored(name);
            }
        }
    }

    public static void enableAttributes(MuleDefinitionParser delegate, String[] attributes)
    {
        enableAttributes(delegate, attributes, true);
    }

    public static void disableAttributes(MuleDefinitionParser delegate, String[] attributes)
    {
        enableAttributes(delegate, attributes, false);
    }

}
