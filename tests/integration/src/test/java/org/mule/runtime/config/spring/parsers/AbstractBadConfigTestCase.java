/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A stripped-down version of FunctionalTestCase that allows us to test the parsing of a bad configuration. 
 */
public abstract class AbstractBadConfigTestCase extends FunctionalTestCase
{
    protected final transient Log logger = LogFactory.getLog(getClass());

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    public void assertErrorContains(String phrase) throws Exception
    {
        try 
        {
            parseConfig();
            fail("expected error");
        } 
        catch (Exception e) 
        {            
            logger.debug("Caught " + e);
            assertTrue("Missing phrase '" + phrase + "' in '" + e.toString() + "'",
                    e.toString().indexOf(phrase) > -1);
        }
    }
    
    protected void parseConfig() throws Exception
    {
        new DefaultMuleContextFactory().createMuleContext(getConfigurationBuilder());
    }

    protected ConfigurationBuilder getConfigurationBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(getConfigFile());
    }
}
