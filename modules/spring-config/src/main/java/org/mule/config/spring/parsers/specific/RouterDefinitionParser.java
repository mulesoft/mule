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
 * Generic router definition parser for parsing all Router elements.
 */
public class RouterDefinitionParser extends ChildDefinitionParser
{

    public static final String ROUTER = "messageProcessor";

    public RouterDefinitionParser(Class clazz)
    {
        super(ROUTER, clazz);
    }

    // specifically for subclasses of AbstractCorrelationAggregator (requires a "class=..." in the config)
    public RouterDefinitionParser()
    {
        super(ROUTER, null, AbstractAggregator.class, true);
    }

}
