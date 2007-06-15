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

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.security.MuleCredentials;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.context.SecurityContextImpl;

public class AuthComponentSynchFunctionalTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "auth-component-synch-test.xml";
    }

    // Clear the security context after each test.
    public void doFunctionalTearDown()
    {
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    public void testCaseGoodAuthenticationGoodAuthorisation() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();

        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("marie", "marie", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Marie", props);
        assertNotNull(m);
        assertTrue(m.getPayload().equals("Marie"));
    }

    public void testCaseGoodAuthenticationBadAuthorisation() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();

        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Marie", props);
        assertFalse(m.getPayload().equals("Marie"));
    }

    public void testCaseBadAuthentication() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();

        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Marie", props);
        assertNotNull(m.getPayload());
    }

}
