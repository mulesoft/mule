/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi;

import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.providers.AuthenticationProvider;
import net.sf.acegisecurity.providers.dao.DaoAuthenticationProvider;
import net.sf.acegisecurity.providers.dao.User;
import net.sf.acegisecurity.providers.dao.memory.InMemoryDaoImpl;
import net.sf.acegisecurity.providers.dao.memory.UserMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.mule.components.simple.EchoComponent;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.MuleProperties;
import org.mule.extras.acegi.filters.http.HttpBasicAuthenticationFilter;
import org.mule.extras.client.MuleClient;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.impl.security.MuleUserAuthenticationToken;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOManager;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.MuleManager;

import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
        assertEquals(100, m.getErrorCode());

    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
        String user = new MuleUserAuthenticationToken("anonX", "anonX".toCharArray()).getToken();
        user = new String(strategy.encrypt(user.getBytes()));
        props.put(MuleProperties.MULE_USER_PROPERTY, user);

        UMOMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        int status = m.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, -1);
        System.out.println(m.getPayload());
        assertEquals(100, m.getErrorCode());
    }

    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = new MuleClient();

        Map props = new HashMap();
        UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
        String user = new MuleUserAuthenticationToken("anon", "anon".toCharArray()).getToken();
        user = new String(strategy.encrypt(user.getBytes()));
        props.put(MuleProperties.MULE_USER_PROPERTY, user);

        UMOMessage m = client.send("vm://my.queue", "foo", props);
        assertNotNull(m);
        assertEquals(0, m.getErrorCode());
        System.out.println(m.getPayload());
    }
}
