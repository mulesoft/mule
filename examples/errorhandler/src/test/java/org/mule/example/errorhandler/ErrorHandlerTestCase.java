/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler;

import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.SystemUtils;

import java.util.Properties;

import org.junit.Test;

public class ErrorHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected Properties getStartUpProperties()
    {
        Properties startupProps = new Properties();
        startupProps.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, SystemUtils.JAVA_IO_TMPDIR);
        return  startupProps;
    }

    @Override
    protected String getConfigFile()
    {
        return "mule-config.xml";
    }

    @Test
    public void testConfigSanity()
    {
        // empty
    }
    
    // TODO Create a test to copy files from test-data/out to test-data/in
}


