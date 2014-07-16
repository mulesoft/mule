/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
