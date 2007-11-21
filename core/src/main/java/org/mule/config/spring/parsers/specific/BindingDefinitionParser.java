/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.GrandchildDefinitionParser;
import org.mule.routing.inbound.CorrelationAggregator;

/**
 * Binding definition parser for parsing all binding elements configured as part of the component.
 */
public class BindingDefinitionParser extends GrandchildDefinitionParser
{

    public BindingDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
        standardOptions();
    }

    // specifically for subclasses of CorrelationAggregator (requires a "class=..." in the config)
    public BindingDefinitionParser(String setterMethod)
    {
        super(setterMethod, null, CorrelationAggregator.class, true);
        standardOptions();
    }

    protected void standardOptions()
    {
        addMapping("enableCorrelation", "IF_NOT_SET=0,ALWAYS=1,NEVER=2");
    }

}
