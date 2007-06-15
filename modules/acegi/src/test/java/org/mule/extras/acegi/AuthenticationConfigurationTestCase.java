/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.Iterator;
import java.util.Map;

public class AuthenticationConfigurationTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "authentication-config.xml";
    }

    public void testAcegi()
    {
        Map endpoints = managementContext.getRegistry().getEndpoints();
        Iterator names = endpoints.keySet().iterator();
        while (names.hasNext())
        {
            String name = (String) names.next();
            UMOEndpoint endpoint = (UMOEndpoint) endpoints.get(name);
            logger.debug(name + " : " + endpoint);
        }
    }

}
