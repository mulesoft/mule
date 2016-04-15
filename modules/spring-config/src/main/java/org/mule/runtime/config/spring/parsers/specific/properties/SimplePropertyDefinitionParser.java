/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
