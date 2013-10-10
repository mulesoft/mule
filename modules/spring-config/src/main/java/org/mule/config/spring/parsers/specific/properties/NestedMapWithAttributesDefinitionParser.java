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
import org.mule.config.spring.parsers.generic.AttributePropertiesDefinitionParser;
import org.mule.config.spring.parsers.processors.AddAttribute;

/**
 * This generates a nested map (an element of the parent map, with the key "mapKey", which is a map itself)
 * and then adds any attributes as name/value pairs.  Embedded elements can then insert further entries using
 * {@link org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser} or, if the entry is
 * just a simple value, {@link org.mule.config.spring.parsers.specific.properties.SimplePropertyDefinitionParser}.
 * The target setter is {@link org.mule.config.spring.parsers.assembly.MapEntryCombiner#VALUE}.
 */
public class NestedMapWithAttributesDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public NestedMapWithAttributesDefinitionParser(String mapSetter, String mapKey)
    {
        addDelegate(new ChildSingletonMapDefinitionParser(mapSetter))
                .addCollection(mapSetter)
                .setIgnoredDefault(true)
                .removeIgnored(MapEntryCombiner.KEY)
                .registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, mapKey));
        addChildDelegate(new AttributePropertiesDefinitionParser(MapEntryCombiner.VALUE))
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME)
                .addIgnored(MapEntryCombiner.KEY);
    }

}

