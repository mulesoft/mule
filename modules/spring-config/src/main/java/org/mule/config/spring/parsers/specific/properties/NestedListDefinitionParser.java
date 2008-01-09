/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.properties;

import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.processors.AddAttribute;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

/**
 * This extends a list that is itself a property (with key mapKey).  It does not have any
 * container element.
 */
public class NestedListDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public NestedListDefinitionParser(String mapSetter, String mapKey, String attribute)
    {
        addDelegate(new ChildSingletonMapDefinitionParser(mapSetter))
                .registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, mapKey))
                .addCollection(mapSetter)
                .setIgnoredDefault(true)
                .removeIgnored(MapEntryCombiner.KEY)
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addChildDelegate(new ChildListEntryDefinitionParser(MapEntryCombiner.VALUE, attribute))
                .addCollection(MapEntryCombiner.VALUE);
    }

}
