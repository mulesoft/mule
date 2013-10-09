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
public class AggregatorDefinitionParser extends ChildDefinitionParser
{

    public static final String SETTER = "messageProcessor";

    public AggregatorDefinitionParser(Class clazz)
    {
        super(SETTER, clazz);
    }

    public AggregatorDefinitionParser()
    {
        super(SETTER, null, AbstractAggregator.class, true);
    }

}
