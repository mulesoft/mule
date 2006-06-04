/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.components.script;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GroovyMessageBuilderTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/components/script/groovy-builder-config.xml";
    }

    public void testFunctionBehaviour() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://groovy.1", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: A Received B Received", m.getPayloadAsString());
    }
}
