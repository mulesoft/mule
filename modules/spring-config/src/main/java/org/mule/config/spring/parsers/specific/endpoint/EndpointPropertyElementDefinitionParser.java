/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.AddAttribute;

/**
 * This parser parses nested endpoint elements adding the resulting beans to the map
 * of properties on the EndpointBuilder rather than attempting to inject them on the
 * EndpointBuilder itself.
 */
public class EndpointPropertyElementDefinitionParser extends AbstractSingleParentFamilyChildDefinitionParser
{
    public static final String ENDPOINT_PROPERTIES_ATTRIBUTE = "properties";

    public EndpointPropertyElementDefinitionParser(String propertyKey, Class beanClass)
    {
        setReturnFirstResult(false);
        addDelegate(new ChildSingletonMapDefinitionParser(ENDPOINT_PROPERTIES_ATTRIBUTE)).registerPreProcessor(
            new AddAttribute(MapEntryCombiner.KEY, propertyKey))
            .addCollection(ENDPOINT_PROPERTIES_ATTRIBUTE)
            .setIgnoredDefault(true)
            .removeIgnored(MapEntryCombiner.KEY)
            .addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addChildDelegate(new ChildDefinitionParser(MapEntryCombiner.VALUE, beanClass)).addIgnored(
            AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME).addIgnored(MapEntryCombiner.KEY);
    }
}
