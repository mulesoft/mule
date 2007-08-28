/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.Collection;
import java.util.Iterator;

public class AcegiAuthenticationNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "acegi-authentication-config.xml";
    }

    public void testAcegi()
    {
        Collection endpoints = managementContext.getRegistry().getEndpoints();
        Iterator it = endpoints.iterator();
        while (it.hasNext())
        {
            UMOEndpoint endpoint = (UMOEndpoint) it.next();
            logger.debug(endpoint.getName() + " : " + endpoint);
        }
    }

}
