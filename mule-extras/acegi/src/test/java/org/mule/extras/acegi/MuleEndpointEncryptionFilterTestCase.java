/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi;

import org.mule.MuleManager;
import org.mule.config.ExceptionHelper;
import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.security.MuleCredentials;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOMessage;
import org.mule.umo.security.CredentialsNotSetException;
import org.mule.umo.security.UnauthorisedException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleEndpointEncryptionFilterTestCase extends NamedTestCase
{
    public void setUp() throws Exception
    {
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("test-acegi-encrypt-config.xml");
    }

    protected void tearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
    }

    public void testAuthenticationFailureNoContext() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://my.queue", "foo", null);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        System.out.println(m.getPayload());
        assertNotNull(m.getExceptionPayload());
        assertEquals(ExceptionHelper.getErrorCode(CredentialsNotSetException.class), m.getExceptionPayload().getCode());

    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
        String user = new MuleCredentials("anonX", "anonX".toCharArray()).getToken();
        user = new String(strategy.encrypt(user.getBytes(), null));
        props.put(MuleProperties.MULE_USER_PROPERTY, user);

        UMOMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        System.out.println(m.getPayload());
        assertNotNull(m.getExceptionPayload());
        assertEquals(ExceptionHelper.getErrorCode(UnauthorisedException.class), m.getExceptionPayload().getCode());

    }

    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
        String user = new MuleCredentials("anon", "anon".toCharArray()).getToken();
        user = new String(strategy.encrypt(user.getBytes(), null));
        props.put(MuleProperties.MULE_USER_PROPERTY, user);

        UMOMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertNull(m.getExceptionPayload());

        System.out.println(m.getPayload());
    }

    public void testAuthenticationFailureBadCredentialsHttp() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
        String user = new MuleCredentials("anonX", "anonX".toCharArray()).getToken();
        user = new String(strategy.encrypt(user.getBytes(), null));
        props.put(MuleProperties.MULE_USER_PROPERTY, user);

        UMOMessage m = client.send("http://localhost:4567/index.html", "", props);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        System.out.println(m.getPayload());
        assertEquals(401, status);
    }

    public void testAuthenticationAuthorisedHttp() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
        String user = new MuleCredentials("anon", "anon".toCharArray()).getToken();
        user = new String(strategy.encrypt(user.getBytes(), null));
        props.put(MuleProperties.MULE_USER_PROPERTY, user);

        UMOMessage m = client.send("http://localhost:4567/index.html", "", props);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        System.out.println(m.getPayload());
        assertEquals(200, status);
    }
}
