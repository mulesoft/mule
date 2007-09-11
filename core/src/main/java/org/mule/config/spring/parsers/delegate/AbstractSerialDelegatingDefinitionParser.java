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
import org.mule.config.spring.parsers.preprocessors.DisableByAttribute;
import org.mule.util.StringUtils;

import java.util.Iterator;

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
 *
 * <p>If {@link org.mule.config.spring.parsers.preprocessors.DisableByAttribute.DisableByAttributeException}
 * is thrown it is trapped (not shown to the user) and the next parser invoked.  This allows programatic
 * selection of parsers (for static selection use the schema).</p>
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
        AbstractBeanDefinition bean = null;
        while (null == bean && index < size())
        {
            try
            {
                bean = getDelegate(index++).parseDelegate(element, parserContext);
            }
            catch (DisableByAttribute.DisableByAttributeException e)
            {
                bean = null;
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

    protected void addDelegate(MuleDefinitionParser delegate)
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

}
