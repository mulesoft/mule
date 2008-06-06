/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.config;

import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.context.DefaultMuleContextFactory;

import junit.framework.TestCase;

public class ScriptingConfigErrorTestCase extends TestCase
{
    public void testMissingEngine() throws InitialisationException
    {
        try
        {
            new DefaultMuleContextFactory().createMuleContext("config-error.xml");
            fail("This config should have thrown an exception because the 'engine' attribute is required");
        }
        catch (ConfigurationException e)
        {
            // expected
        }
    }    
}


