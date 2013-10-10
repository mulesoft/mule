/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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


