/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.providers.email;

import org.mule.providers.email.SmtpConnector;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SmtpConnectorTestCase extends Pop3ConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        SmtpConnector c = new SmtpConnector();
        c.setName("Smtpconnector");
        c.initialise();
        return c;
    }

    public String getTestEndpointURI()
    {
        return "smtp://a:a@a.com";
    }

    public void testReceiver() throws Exception
    {
        // override to disable parent implementation
    }

}
