/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.ConfigurationBuilder;

/**
 * Test Case for MuleClasspathCofigurationBuilder. It borrows everything from the
 * corresponding MuleXml config builder and just substitute the builder itself since
 * the config files are in the tests classpath.
 */
public class MuleClasspathConfigBuilderTestCase extends MuleXmlConfigBuilderTestCase
{
    /**
     * Get the builder
     */
    public ConfigurationBuilder getBuilder()
    {
//        try
//        {
            //TODO RM* URGENT
            return null; // new MuleClasspathConfigurationBuilder();
//        }
//        catch (ConfigurationException e)
//        {
//            fail(e.getMessage());
//            return null;
//        }
    }
}
