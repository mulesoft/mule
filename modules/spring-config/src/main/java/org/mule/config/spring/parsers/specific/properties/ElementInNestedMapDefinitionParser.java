/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
