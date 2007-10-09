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

import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;

public class SingleParentFamilyDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public SingleParentFamilyDefinitionParser(MuleDefinitionParser parent)
    {
        // avoid the overriden method here
        super.addDelegate(parent);
        parent.setIgnoredDefault(false);
    }

    protected MuleDefinitionParser addDelegate(MuleDefinitionParser delegate)
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

