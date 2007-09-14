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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SimpleSingleParentFamilyDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    private Map attributeNames = new HashMap();

    public SimpleSingleParentFamilyDefinitionParser(MuleDefinitionParser parent)
    {
        // avoid the overriden method here
        super.addDelegate(parent);
        parent.setIgnoredDefault(false);
    }

    protected void addDelegate(MuleDefinitionParser delegate)
    {
        throw new UnsupportedOperationException("Delegates must be associated with attribute names");
    }

    public SimpleSingleParentFamilyDefinitionParser addDelegate(String attribute, MuleDefinitionParser delegate)
    {
        return addDelegate(new String[]{attribute}, delegate);
    }

    public SimpleSingleParentFamilyDefinitionParser addDelegate(String[] attributes, MuleDefinitionParser delegate)
    {
        attributeNames.put(delegate, Arrays.asList(attributes));
        super.addDelegate(delegate);
        delegate.setIgnoredDefault(true);
        for (int i = 0; i < attributes.length; i++)
        {
            getDelegate(0).addIgnored(attributes[i]);
            delegate.removeIgnored(attributes[i]);
        }
        return this;
    }

}

