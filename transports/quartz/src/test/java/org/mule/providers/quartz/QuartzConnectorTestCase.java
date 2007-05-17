/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class QuartzConnectorTestCase extends AbstractConnectorTestCase
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getConnector()
     */
    public UMOConnector getConnector() throws Exception
    {
        QuartzConnector c = new QuartzConnector();
        c.setName("QuartzConnector");
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getValidMessage()
     */
    public Object getValidMessage() throws Exception
    {
        return "test";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getTestEndpointURI()
     */
    public String getTestEndpointURI()
    {
        return "quartz:/myService?repeatInterval=1000";
    }
}
