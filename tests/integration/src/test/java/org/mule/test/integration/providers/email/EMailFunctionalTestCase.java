/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.email;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class EMailFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/providers/email/email-config.xml";
    }

    public void testRoundtrip() throws Exception
    {
        MuleClient mc = new MuleClient();
        String messageString = "test";
        Map props = new HashMap();
        
        mc.sendNoReceive("smtp://eat@deliasystems.com", messageString, props);
        UMOMessage msg = mc.receive("pop3://eat%40delisys4.fr.fto:kIxUjC6R@pop.fr.oleane.com", 10000);
        assertNotNull(msg);
    }

}
