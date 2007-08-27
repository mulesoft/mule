/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jnp;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class JnpConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public UMOConnector createConnector() throws Exception
    {
        JnpConnector c = new JnpConnector();
        c.setName("JnpConnector");
        c.setSecurityManager(null);
        return c;
    }

    public String getTestEndpointURI()
    {
        return "jnp://localhost:1099";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

}
