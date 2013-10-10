/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    private boolean returnFirstResult = true;

    public AbstractFirstResultSerialDefinitionParser()
    {
        super();
    }

    public AbstractFirstResultSerialDefinitionParser(boolean doReset)
    {
        super(doReset);
    }

    public void setReturnFirstResult(boolean returnFirstResult)
    {
        this.returnFirstResult = returnFirstResult;
    }

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
                if (returnFirstResult)
                {
                    return firstDefinition;
                }
                else
                {
                    return result;
                }
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
