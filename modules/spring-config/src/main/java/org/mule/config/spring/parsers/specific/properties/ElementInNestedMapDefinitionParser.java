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
 * This constructs a nested map - keyed by "mapKey" - and then adds an entry in that
 * named from the attribute "keyAttribute".  Child elements can then set a value (or
 * values, if it is a collection) on {@link org.mule.config.spring.parsers.assembly.MapEntryCombiner#VALUE}
 */
public class ElementInNestedMapDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public ElementInNestedMapDefinitionParser(String mapSetter, String mapKey, String keyAttribute)
    {
        // children (parameters) want to append to the inner map, not the outer one
        setReturnFirstResult(false);
        addDelegate(new ChildSingletonMapDefinitionParser(mapSetter))
                .registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, mapKey))
                .addCollection(mapSetter)
                .setIgnoredDefault(true)
                .removeIgnored(MapEntryCombiner.KEY)
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addChildDelegate(new ChildSingletonMapDefinitionParser(MapEntryCombiner.VALUE))
                .addAlias(keyAttribute, MapEntryCombiner.KEY)
                .addCollection(MapEntryCombiner.VALUE)
                .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
    }

}
