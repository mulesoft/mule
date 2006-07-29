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
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.SystemUtils;
import org.mule.components.simple.EchoComponent;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.extras.acegi.filters.http.HttpBasicAuthenticationFilter;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.providers.http.HttpConstants;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.security.UMOSecurityProvider;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpBasicEndpointFilterTestCase extends FunctionalTestCase
{

    protected String getConfigResources() {
        return "";
    }

    protected ConfigurationBuilder getBuilder() throws Exception {
        MuleSecurityManager sm = new MuleSecurityManager();
        UMOSecurityProvider provider = new AcegiProviderAdapter(getTestProvider(), "testProvider");
        sm.addProvider(provider);
        QuickConfigurationBuilder builder = null;
        builder = new QuickConfigurationBuilder(true);
        UMOManager manager = builder.createStartedManager(true, "");
        manager.setSecurityManager(sm);
        UMODescriptor d = builder.createDescriptor(EchoComponent.class.getName(),
                                                   "echo",
                                                   "http://localhost:4567",
                                                   null,
                                                   null);
        d.getInboundEndpoint().setSecurityFilter(new HttpBasicAuthenticationFilter("mule-realm"));
        builder.registerComponent(d);

        return builder;
    }

    public AuthenticationProvider getTestProvider() throws Exception
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        InMemoryDaoImpl dao = new InMemoryDaoImpl();
        UserMap map = new UserMap();
        map.addUser(new User("ross",
                             "ross",
                             true,
                             true,
                             true,
                             new GrantedAuthority[] { new GrantedAuthorityImpl("ROLE_ADMIN") }));
        map.addUser(new User("anon",
                             "anon",
                             true,
                             true,
                             true,
                             new GrantedAuthority[] { new GrantedAuthorityImpl("ROLE_ANONYOMUS") }));
        dao.setUserMap(map);
        dao.afterPropertiesSet();
        provider.setAuthenticationDao(dao);
        return provider;
    }


    public void testAuthenticationFailureNoContext() throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        GetMethod get = new GetMethod("http://localhost:4567/index.html");

        get.setDoAuthentication(false);

        try {
            int status = client.executeMethod(get);

            assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
            System.out.println(status + SystemUtils.LINE_SEPARATOR + get.getResponseBodyAsString());

        } finally {
            get.releaseConnection();
        }
    }

    public void testAuthenticationFailureBadCredentials() throws Exception
    {
        doRequest(null, "localhost", "anonX", "anonX", "http://localhost:4567/index.html", true, false, 401);
    }

    public void testAuthenticationAuthorised() throws Exception
    {
        doRequest(null, "localhost", "anon", "anon", "http://localhost:4567/index.html", false, true, 200);
    }

    public void testAuthenticationAuthorisedWithHandshake() throws Exception
    {
        doRequest(null, "localhost", "anon", "anon", "http://localhost:4567/index.html", true, false, 200);
    }

    public void testAuthenticationAuthorisedWithHandshakeAndBadRealm() throws Exception
    {
        doRequest("blah", "localhost", "anon", "anon", "http://localhost:4567/index.html", true, false, 401);
    }

    public void testAuthenticationAuthorisedWithHandshakeAndRealm() throws Exception
    {
        doRequest("mule-realm", "localhost", "ross", "ross", "http://localhost:4567/index.html", true, false, 200);
    }

    private void doRequest(String realm,
                           String host,
                           String user,
                           String pass,
                           String url,
                           boolean handshake,
                           boolean preemtive,
                           int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(preemtive);
        client.getState().setCredentials(new AuthScope(host, -1, realm), new UsernamePasswordCredentials(user, pass));
        GetMethod get = new GetMethod(url);
        get.setDoAuthentication(handshake);

        try {
            int status = client.executeMethod(get);
            assertEquals(result, status);
            System.out.println(status + SystemUtils.LINE_SEPARATOR + get.getResponseBodyAsString());

        } finally {
            get.releaseConnection();
        }
    }

}
