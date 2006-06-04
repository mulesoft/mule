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
package org.mule.providers.rmi;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author <a href="mailto:fsweng@bass.com.my">fs Weng</a>
 * @version $Revision$
 */

public class RmiConnectorTestCase extends AbstractConnectorTestCase
{
    public UMOConnector getConnector() throws Exception
    {
        RmiConnector c = new RmiConnector();
        c.setName("RmiConnector");
        c.setSecurityManager(null);
        c.initialise();
        return c;
    }

    public String getTestEndpointURI()
    {
        return "rmi://localhost:1099";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testProperties() throws Exception
    {
        RmiConnector c = (RmiConnector) connector;

        String securityPolicy = "rmi.policy";
        String serverCodebase = "file:///E:/projects/MyTesting/JAVA/rmi/classes/";

        c.setSecurityPolicy(securityPolicy);
        assertNotNull(c.getSecurityPolicy());
        c.setServerCodebase(serverCodebase);
        assertEquals(serverCodebase, c.getServerCodebase());
    }

    public void testSetMethodArgumentTypes() throws Exception
    {
        RmiConnector c = (RmiConnector) connector;

        ArrayList list = null;

        c.setMethodArgumentTypes(list);

        list = new ArrayList(Arrays.asList(new Object[] { "java.lang.String", "java.rmi.Remote" }));
        c.setMethodArgumentTypes(list);

        Class classes[] = c.getArgumentClasses();

        for (int i = 0; i < classes.length; i++) {
            String argTypeString = (String) list.get(i);
            assertEquals(argTypeString, classes[i].getName());
        }
    }

}
