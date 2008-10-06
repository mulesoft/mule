/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

import org.mule.tck.FunctionalTestCase;

public class MultiConnectorMULE3669TestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "multiconnector-mule3669-test.xml";
    }

    public void testInitialisation() throws Exception
    {
        //Just need to ensure the test sets up connrectly
    }
}
