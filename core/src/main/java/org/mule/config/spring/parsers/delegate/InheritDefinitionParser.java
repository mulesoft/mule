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

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.AutoIdUtils;

import org.w3c.dom.Element;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * This encapsulates two definition parsers - orphan and named - and returns the
 * named definition parser if the "inherit" attribute is set.  This allows a named
 * orphan to be defined (inherit="false") and then extended (inherit="true").
 * The two sub-parsers must be consistent, as described in
 * {@link AbstractParallelDelegatingDefinitionParser}
 */
public class InheritDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    public static final String INHERIT = "inherit";
    private OrphanDefinitionParser orphan;
    private NamedDefinitionParser named;

    public InheritDefinitionParser(OrphanDefinitionParser orphan, NamedDefinitionParser named)
    {
        super(new DelegateDefinitionParser[]{orphan, named});
        this.orphan = orphan;
        this.named = named;
        addIgnored(INHERIT);
    }

    protected DelegateDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        // i'm not sure why this is suddenly necessary here and not elsewhere.
        // perhaps because this is used on the top level but has name deleted?
        AutoIdUtils.ensureUniqueId(element);
        if (null != element && element.hasAttribute(INHERIT)
                && Boolean.valueOf(element.getAttribute(INHERIT)).booleanValue())
        {
            return named;
        }
        else
        {
            return orphan;
        }
    }

}
