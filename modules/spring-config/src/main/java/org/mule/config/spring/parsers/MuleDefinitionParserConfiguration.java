/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.configuration.ValueMap;

import java.util.Map;

/**
 * @see org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration
 */
public interface MuleDefinitionParserConfiguration
{

    /**
     * These are prepended to existing processors
     * @param preProcessor
     */
    MuleDefinitionParserConfiguration registerPreProcessor(PreProcessor preProcessor);

    /**
     * These are appended to existing processors
     * @param postProcessor
     */
    MuleDefinitionParserConfiguration registerPostProcessor(PostProcessor postProcessor);

    MuleDefinitionParserConfiguration addReference(String propertyName);

    MuleDefinitionParserConfiguration addMapping(String propertyName, Map mappings);

    MuleDefinitionParserConfiguration addMapping(String propertyName, String mappings);

    MuleDefinitionParserConfiguration addMapping(String propertyName, ValueMap mappings);

    MuleDefinitionParserConfiguration addAlias(String alias, String propertyName);

    MuleDefinitionParserConfiguration addCollection(String propertyName);

    MuleDefinitionParserConfiguration addIgnored(String propertyName);

    MuleDefinitionParserConfiguration removeIgnored(String propertyName);

    MuleDefinitionParserConfiguration setIgnoredDefault(boolean ignoreAll);

    MuleDefinitionParserConfiguration addBeanFlag(String flag);

}
