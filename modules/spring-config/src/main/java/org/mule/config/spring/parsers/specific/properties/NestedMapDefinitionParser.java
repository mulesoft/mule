/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.properties;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.processors.AddAttribute;

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
