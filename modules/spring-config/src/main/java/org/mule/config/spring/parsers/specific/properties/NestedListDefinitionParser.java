/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.properties;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.processors.AddAttribute;

/**
 * This extends a list that is itself a property (with key mapKey).  It does not have any
 * container element.
 *
 * This could also be achieved with
 * new ChildSingletonMapDefinitionParser("properties")
 * .registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, "soap11Transports"))
 * .addCollection(MapEntryCombiner.VALUE)
 * .addCollection("properties");
 * I think, but the following avoids worries about special attribute names.
 */
public class NestedListDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    // we use this so that they can be used as the attribute name!
    public static final String HIDDEN_KEY = "hiddenKey";
    public static final String HIDDEN_VALUE = "hiddenValue";

    public NestedListDefinitionParser(String mapSetter, String mapKey, String attribute)
    {
        addDelegate(new ChildSingletonMapDefinitionParser(mapSetter))
                .registerPreProcessor(new AddAttribute(HIDDEN_KEY, mapKey))
                .addCollection(mapSetter)
                .setIgnoredDefault(true)
                .addAlias(HIDDEN_KEY, MapEntryCombiner.KEY)
                .removeIgnored(HIDDEN_KEY)
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addChildDelegate(new ChildListEntryDefinitionParser(HIDDEN_VALUE, attribute))
                .addAlias(HIDDEN_VALUE, MapEntryCombiner.VALUE)
                .addCollection(HIDDEN_VALUE);
    }

}
