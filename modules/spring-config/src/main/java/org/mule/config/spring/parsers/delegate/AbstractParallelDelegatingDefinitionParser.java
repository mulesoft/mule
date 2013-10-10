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
 * This allows a definition parsers to be dynamically represented by one instance
 * selected from a set of parsers, depending on the context.  For example, a single
 * Mule model may be defined across several file - the first file used defines the
 * model and subsequent uses extend it (for this particular case, see
 * {@link InheritDefinitionParser}).
 *
 * <p>Note that the sub-parsers must be consistent.  That includes matching the
 * same schema, for example.
 */
public abstract class AbstractParallelDelegatingDefinitionParser extends AbstractDelegatingDefinitionParser
{

    protected AbstractParallelDelegatingDefinitionParser()
    {
        super();
    }

    protected AbstractParallelDelegatingDefinitionParser(MuleDefinitionParser[] delegates)
    {
        super(delegates);
    }

    public AbstractBeanDefinition muleParse(Element element, ParserContext parserContext)
    {
        return getDelegate(element, parserContext).muleParse(element, parserContext);
    }

    protected abstract MuleDefinitionParser getDelegate(Element element, ParserContext parserContext);

}
