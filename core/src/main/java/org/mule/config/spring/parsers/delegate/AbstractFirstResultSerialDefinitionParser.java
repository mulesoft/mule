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

import org.mule.config.spring.parsers.MuleDefinitionParser;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Extend {@link org.mule.config.spring.parsers.delegate.AbstractSerialDelegatingDefinitionParser}
 * to return the first definition as the final result
 */
public class AbstractFirstResultSerialDefinitionParser extends AbstractSerialDelegatingDefinitionParser
{

    protected AbstractBeanDefinition firstDefinition;

    protected AbstractBeanDefinition doSingleBean(int index, MuleDefinitionParser parser, Element element, ParserContext parserContext)
    {
        try
        {
            AbstractBeanDefinition result = null;
            try
            {
                result = super.doSingleBean(index, parser, element, parserContext);
            }
            catch (RuntimeException e)
            {
                if (!isExceptionHandled(e))
                {
                    throw e;
                }
            }
            if (0 == index)
            {
                firstDefinition = result;
            }
            if (size() == index + 1)
            {
                return firstDefinition;
            }
            else
            {
                return null;
            }
        }
        catch (RuntimeException e)
        {
            firstDefinition = null;
            throw e;
        }
    }

}
