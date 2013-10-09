/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.properties;

import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;

import org.w3c.dom.Element;

public class SimplePropertyDefinitionParser extends ChildSingletonMapDefinitionParser
{

    public SimplePropertyDefinitionParser()
    {
        super(MapEntryCombiner.VALUE);
    }

    protected void preProcess(Element element)
    {
        super.preProcess(element);
        // this is crazy, but because we use a single property config for target and bean, and both
        // are the same, and target properties are transient, and we only want target value to be
        // a collection, we have to do this here!
        getTargetPropertyConfiguration().addCollection(MapEntryCombiner.VALUE);
    }
    
}
