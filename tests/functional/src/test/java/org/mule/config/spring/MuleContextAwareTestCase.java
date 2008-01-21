/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.tck.FunctionalTestCase;

/**
 * This tests that we can have references to management context aware objects within a config
 */
public class MuleContextAwareTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "management-context-aware-test.xml";
    }

    public void testStartup()
    {
        // only want startup to succeed
    }

}
