/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows several parsers to be used on a single element, creating a parent bean with
 * the first parser and then extending that with child parsers.
 *
 * <p>Note that this make a lot of assumptions about what type of parser is used.
 */
public abstract class AbstractSingleParentFamilyDefinitionParser
        extends AbstractFirstResultSerialDefinitionParser
{

    public AbstractSingleParentFamilyDefinitionParser()
    {
        super();
    }

    public AbstractSingleParentFamilyDefinitionParser(boolean doReset)
    {
        super(doReset);
    }

    protected MuleChildDefinitionParser addChildDelegate(MuleChildDefinitionParser delegate)
    {
        return (MuleChildDefinitionParser) super.addDelegate(delegate);
    }

    protected MuleDefinitionParserConfiguration addDelegate(MuleDefinitionParser delegate)
    {
        if (size() > 0)
        {
            return addDelegateAsChild(delegate);
        }
        else
        {
            return super.addDelegate(delegate);
        }
    }

    protected MuleDefinitionParserConfiguration addDelegateAsChild(MuleDefinitionParser delegate)
    {
        if (delegate instanceof MuleChildDefinitionParser)
        {
            return addChildDelegate((MuleChildDefinitionParser) delegate);
        }
        else
        {
            throw new IllegalStateException("Children must implement child interface");
        }
    }

    protected AbstractBeanDefinition doSingleBean(int index, MuleDefinitionParser parser,
                                                  Element element, ParserContext parserContext)
    {
        if (0 != index)
        {
            ((MuleChildDefinitionParser) parser).forceParent(firstDefinition);
            // we need this because we often block "everything but" which would mean
            // being unable to set ourselves on the parent
            ((MuleChildDefinitionParser) parser).getTargetPropertyConfiguration().setIgnoredDefault(false);
        }
        return super.doSingleBean(index, parser, element, parserContext);
    }

}
