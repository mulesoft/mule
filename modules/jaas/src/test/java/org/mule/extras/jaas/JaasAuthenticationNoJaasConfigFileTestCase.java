/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.security.MuleCredentials;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class JaasAuthenticationNoJaasConfigFileTestCase extends FunctionalTestCase
{

    public JaasAuthenticationNoJaasConfigFileTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }
    
    public void assertExceptionPayload(UMOMessage umoMessage, String exceptionMessage)
    {
        assertNotNull(umoMessage.getExceptionPayload());
        UMOExceptionPayload exceptionPayload = umoMessage.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertEquals(exceptionMessage, exceptionPayload.getMessage());
    }

    public void testCaseGoodAuthentication() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "dragon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayloadAsString().equals("Test Received"));
    }

    public void testCaseDifferentGoodAuthentication() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayloadAsString().equals("Test Received"));
    }

    public void testCaseWrongCombinationOfCorrectUsernameAndPassword() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertFalse(m.getPayloadAsString().equals("Test Received"));
        
        assertExceptionPayload(m, "Authentication failed for principal Marie.Rizzo. Message payload is of type: String");
        
    }

    public void testCaseBadUserName() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Evil", "dragon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertFalse(m.getPayloadAsString().equals("Test Received"));
        
        assertExceptionPayload(m, "Authentication failed for principal Evil. Message payload is of type: String");
    }

    public void testCaseBadPassword() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "evil", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        UMOMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertFalse(m.getPayloadAsString().equals("Test Received"));
        
        assertExceptionPayload(m, "Authentication failed for principal Marie.Rizzo. Message payload is of type: String");
    }

    protected String getConfigResources()
    {
        return "mule-conf-with-no-jaas-config-file.xml";
    }
}
