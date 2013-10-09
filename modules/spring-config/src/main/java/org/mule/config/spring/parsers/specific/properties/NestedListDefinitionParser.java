/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
