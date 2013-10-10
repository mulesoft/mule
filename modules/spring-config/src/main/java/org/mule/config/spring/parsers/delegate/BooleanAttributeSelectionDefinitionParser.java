/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.MuleDefinitionParser;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

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
