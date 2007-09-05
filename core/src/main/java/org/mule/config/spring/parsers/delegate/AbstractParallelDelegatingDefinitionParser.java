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

    protected AbstractParallelDelegatingDefinitionParser(DelegateDefinitionParser[] delegates)
    {
        super(delegates);
    }

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext)
    {
        return getDelegate(element, parserContext).parseDelegate(element, parserContext);
    }

    protected abstract DelegateDefinitionParser getDelegate(Element element, ParserContext parserContext);

}
