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

import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.MuleDefinitionParser;

import org.w3c.dom.Element;
import org.springframework.beans.factory.xml.ParserContext;

public class BooleanAttributeSelectionDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    private String attribute;
    private boolean dflt;
    private MuleDefinitionParser whenTrue;
    private MuleDefinitionParser whenFalse;

    public BooleanAttributeSelectionDefinitionParser(String attribute, boolean dflt, MuleDefinitionParser whenTrue, MuleDefinitionParser whenFalse)
    {
        super(new MuleDefinitionParser[]{whenTrue, whenFalse});
        this.attribute = attribute;
        this.dflt = dflt;
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
        addIgnored(attribute);
    }

    protected MuleDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        // i'm not sure why this is suddenly necessary here and not elsewhere.
        // perhaps because this is used on the top level but has name deleted?
        AutoIdUtils.ensureUniqueId(element, attribute);
        boolean value = dflt;
        if (null != element && element.hasAttribute(attribute))
        {
            value = Boolean.valueOf(element.getAttribute(attribute)).booleanValue();
        }
        if (value)
        {
            return whenTrue;
        }
        else
        {
            return whenFalse;
        }
    }

}
