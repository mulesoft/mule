/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.config;

import org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.config.spring.parsers.processors.NamedSetterChildElementIterator;
import org.mule.config.spring.parsers.processors.AddAttribute;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;

/**
 * This parser allows us to inter-operate with third party (Apache CXF) bean definition parsers.
 * It "calls out" to the parsers for the child elements and sets them as a list of entries in
 * a map (key "features") which is set on "properties".
 */
public class FeaturesDefinitionParser extends ChildSingletonMapDefinitionParser
{

    public static final String PROPERTIES = "properties";
    public static final String FEATURES = "features";

    public FeaturesDefinitionParser()
    {
        super(PROPERTIES);
        addCollection(PROPERTIES);
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
        PropertyConfiguration configuration = new SimplePropertyConfiguration();
        configuration.addCollection(MapEntryCombiner.VALUE);
        registerPreProcessor(new AddAttribute(MapEntryCombiner.KEY, FEATURES));
        registerPostProcessor(
                new NamedSetterChildElementIterator(
                        MapEntryCombiner.VALUE, new DefaultBeanAssemblerFactory(), configuration));
    }

}
