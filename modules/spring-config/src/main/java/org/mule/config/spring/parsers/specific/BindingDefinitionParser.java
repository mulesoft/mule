/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.routing.AbstractAggregator;

/**
 * Binding definition parser for parsing all binding elements configured as part of the service.
 */
public class BindingDefinitionParser extends ChildDefinitionParser
{

    public BindingDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
        standardOptions();
    }

    // specifically for subclasses of AbstractCorrelationAggregator (requires a "class=..." in the config)
    public BindingDefinitionParser(String setterMethod)
    {
        super(setterMethod, null, AbstractAggregator.class, true);
        standardOptions();
    }

    protected void standardOptions()
    {
        addMapping("enableCorrelation", "IF_NOT_SET=0,ALWAYS=1,NEVER=2");
    }

}
