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

package org.mule.providers.soap.xfire;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireConnectorTestCase extends AbstractConnectorTestCase
{
    public String getTestEndpointURI()
    {
        return "xfire:http://localhost:38009/xfire";
    }

    public UMOConnector getConnector() throws Exception
    {
        XFireConnector c = new XFireConnector();
        c.initialise();
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }
}
