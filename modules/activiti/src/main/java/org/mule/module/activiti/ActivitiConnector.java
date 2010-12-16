/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.activiti;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.http.HttpConnector;

public class ActivitiConnector extends HttpConnector
{
    public static final String ACTIVITI = "activiti";

    private String activitiServerURL;

    private String username;

    private String password;

    private URL url;

    public ActivitiConnector(MuleContext context)
    {
        super(context);
    }

    protected HttpClient getClient()
    {
        HttpClient client = new HttpClient();
        client.setState(new HttpState());
        client.setHttpConnectionManager(this.clientConnectionManager);

        return client;
    }

    public void prepareMethod(HttpMethod httpMethod, HttpClient client) throws UnsupportedEncodingException
    {
        httpMethod.setDoAuthentication(true);

        String authScopeHost = url.getHost();
        int authScopePort = url.getPort();
        String authScopeRealm = AuthScope.ANY_REALM;
        String authScopeScheme = AuthScope.ANY_SCHEME;

        client.getState().setCredentials(
            new AuthScope(authScopeHost, authScopePort, authScopeRealm, authScopeScheme),
            new UsernamePasswordCredentials(this.getUsername(), new String(this.getPassword())));

        client.getParams().setAuthenticationPreemptive(true);
    }

    /**
     * {@inheritDoc}
     */
    public String getProtocol()
    {
        return ACTIVITI;
    }

    public URL getActivitiServerURLA()
    {
        return url;
    }

    public String getActivitiServerURL()
    {
        return activitiServerURL;
    }

    public void setActivitiServerURL(String activitiServerURL)
    {
        this.activitiServerURL = activitiServerURL;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        try
        {
            url = new URL(this.getActivitiServerURL());
        }
        catch (MalformedURLException e)
        {
            throw new InitialisationException(
                CoreMessages.initialisationFailure("Invalid URL: " + this.getActivitiServerURL()), this);
        }
    }
}
