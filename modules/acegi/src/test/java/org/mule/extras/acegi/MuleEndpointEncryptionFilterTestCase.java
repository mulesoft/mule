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

import org.mule.config.ExceptionHelper;
import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.security.MuleCredentials;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOMessage;
import org.mule.umo.security.CredentialsNotSetException;
import org.mule.umo.security.UnauthorisedException;

import java.util.HashMap;
import java.util.Map;

public class MuleEndpointEncryptionFilterTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "test-acegi-encrypt-config.xml";
    }

    public void testAuthenticationFailureNoContext() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://my.queue", "foo", null);
        assertNotNull(m);
        assertNotNull(m.getExceptionPayload());
        assertEquals(ExceptionHelper.getErrorCode(CredentialsNotSetException.class), m.getExceptionPayload()
            .getCode());
    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        UMOMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertNotNull(m.getExceptionPayload());
        assertEquals(ExceptionHelper.getErrorCode(UnauthorisedException.class), m.getExceptionPayload()
            .getCode());
    }

    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        UMOMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertNull(m.getExceptionPayload());
    }

    public void testAuthenticationFailureBadCredentialsHttp() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        UMOMessage m = client.send("http://localhost:4567/index.html", "", props);
        assertNotNull(m);

        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
    }

    public void testAuthenticationAuthorisedHttp() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = managementContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        UMOMessage m = client.send("http://localhost:4567/index.html", "", props);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        assertEquals(HttpConstants.SC_OK, status);
    }

}
