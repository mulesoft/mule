/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.errorhandler;

import org.mule.tck.FunctionalTestCase;

public class ErrorHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "error-config.xml";
    }

    public void testConfigSanity()
    {
        // empty
    }
    
    // TODO Create a test to copy files from test-data/out to test-data/in
}


