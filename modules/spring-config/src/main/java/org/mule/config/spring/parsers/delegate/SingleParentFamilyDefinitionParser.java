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

public class SingleParentFamilyDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public SingleParentFamilyDefinitionParser(MuleDefinitionParser parent)
    {
        // avoid the overriden method here
        super.addDelegate(parent);
        parent.setIgnoredDefault(false);
    }

    protected MuleDefinitionParserConfiguration addDelegate(MuleDefinitionParser delegate)
    {
        throw new UnsupportedOperationException("Delegates must be associated with attribute names");
    }

    public SingleParentFamilyDefinitionParser addChildDelegate(String attribute, MuleChildDefinitionParser delegate)
    {
        return addChildDelegate(new String[]{attribute}, delegate);
    }

    public SingleParentFamilyDefinitionParser addChildDelegate(String[] attributes, MuleChildDefinitionParser delegate)
    {
        super.addChildDelegate(delegate);
        delegate.setIgnoredDefault(true);
        for (int i = 0; i < attributes.length; i++)
        {
            getDelegate(0).addIgnored(attributes[i]);
            delegate.removeIgnored(attributes[i]);
        }
        return this;
    }

}

