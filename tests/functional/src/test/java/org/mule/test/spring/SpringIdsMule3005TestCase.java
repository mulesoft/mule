/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;


/**
 * Pulled from {@link SpringNamespaceConfigBuilderV2TestCase} so that we can disable just the failing test.
 */
public class SpringIdsMule3005TestCase extends AbstractConfigBuilderTestCase
{

    public SpringIdsMule3005TestCase()
    {
        super(true);
    }

    public String getConfigResources()
    {
        return "org/mule/test/spring/config2/test-xml-mule2-config.xml," +
                "org/mule/test/spring/config2/test-xml-mule2-config-split.xml," +
                "org/mule/test/spring/config2/test-xml-mule2-config-split-properties.xml";
    }

    // @Override
    public ConfigurationBuilder getBuilder() throws ConfigurationException
    {
        return new SpringXmlConfigurationBuilder(getConfigResources());
    }

    public void testIds()
    {
        assertEquals("serverId", muleContext.getId());
        assertEquals("clusterId", muleContext.getClusterId());
        assertEquals("domainId", muleContext.getDomain());
    }

}