/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi.config;

import org.mule.config.spring.parsers.generic.SimpleChildDefinitionParser;
import org.mule.extras.acegi.AcegiProviderAdapter;

import org.w3c.dom.Element;

public class AcegiProviderDefinitionParser extends SimpleChildDefinitionParser
{

    public AcegiProviderDefinitionParser()
    {
        super("provider", AcegiProviderAdapter.class);
    }

    public boolean isCollection(Element element)
    {
        return true;
    }
    
}
