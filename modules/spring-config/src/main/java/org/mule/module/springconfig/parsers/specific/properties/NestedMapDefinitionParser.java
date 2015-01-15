/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific.properties;

import org.mule.module.springconfig.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.module.springconfig.parsers.assembly.MapEntryCombiner;
import org.mule.module.springconfig.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.module.springconfig.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.module.springconfig.parsers.processors.AddAttribute;

/**
 * This extends a map that is itself a property (with key mapKey).  It does not have any
 * container element.
 */
public class NestedMapDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    // we use this so that "key" can be used as the attribute name!
    public static final String HIDDEN_KEY = "hiddenKey";

    public NestedMapDefinitionParser(String mapSetter, String mapKey)
    {
        addDelegate(new ChildSingletonMapDefinitionParser(mapSetter))
                .registerPreProcessor(new AddAttribute(HIDDEN_KEY, mapKey))
                .addCollection(mapSetter)
                .setIgnoredDefault(true)
                .addAlias(HIDDEN_KEY, MapEntryCombiner.KEY)
                .removeIgnored(HIDDEN_KEY)
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addChildDelegate(new SimplePropertyDefinitionParser())
                .addIgnored(HIDDEN_KEY)
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
    }

}
