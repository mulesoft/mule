/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class GroovyMessageBuilderTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "groovy-builder-config.xml";
    }

    public void testFunctionBehaviour() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://groovy.1", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: A Received B Received", m.getPayloadAsString());
    }

}
