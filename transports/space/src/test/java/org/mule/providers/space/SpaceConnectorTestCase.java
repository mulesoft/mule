/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.space;

import org.mule.impl.space.VMSpaceFactory;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

/**
 * @version $Revision$
 */

public class SpaceConnectorTestCase extends AbstractConnectorTestCase
{
    /**
     * Create and initialise an instance of your connector here. Do not actually call the
     * connect method.
     */
    public UMOConnector getConnector() throws Exception
    {
        SpaceConnector con = new SpaceConnector();
        con.setName("spaceConnector");
        con.setSpaceFactory(new VMSpaceFactory());
        con.initialise();
        return con;
    }

    public String getTestEndpointURI()
    {
        return "space:rmi://localhoast:1000/MyContainer/JavaSpaces";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello";
    }

    public void testProperties() throws Exception
    {
        // TODO
    }

}
