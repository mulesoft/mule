/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.providers.stream;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SystemStreamConnectorTestCase extends AbstractConnectorTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#createConnector()
     */
    public UMOConnector createConnector() throws Exception
    {
        UMOConnector connector = new SystemStreamConnector();
        connector.setName("TestStream");
        connector.initialise();
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "stream://System.out";
    }


    public UMOConnector getConnector() throws Exception
    {
        UMOConnector cnn = new SystemStreamConnector();
        cnn.setName("TestStream");
        cnn.initialise();
        return cnn;
    }

    public Object getValidMessage() throws Exception
    {
        return "Test Message";
    }
}
