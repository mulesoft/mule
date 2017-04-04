/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.sftp.config.SftpProxyConfig;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.log4j.Logger;

public class SftpConnectionFactory implements PoolableObjectFactory
{
    private final static Logger logger = Logger.getLogger(SftpConnectionFactory.class);

    private final ImmutableEndpoint endpoint;
    private String preferredAuthenticationMethods;
    private SftpProxyConfig proxyConfig;

    public SftpConnectionFactory(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public void activateObject(Object o) throws Exception
    {
        // Do nothing!
    }

    @Override
    public void destroyObject(Object o) throws Exception
    {
        SftpClient client = (SftpClient) o;
        client.disconnect();
    }

    @Override
    public Object makeObject() throws Exception
    {
        return createClient();
    }

    public SftpClient createClient() throws IOException
    {
        EndpointURI endpointURI = endpoint.getEndpointURI();

        String host = endpointURI.getHost();
        if (logger.isDebugEnabled())
        {
            logger.debug("Using host: " + host);
        }

        SftpClient client = new SftpClient(host);
        if (!StringUtils.isEmpty(preferredAuthenticationMethods))
        {
            client.setPreferredAuthenticationMethods(preferredAuthenticationMethods);
        }
        if (proxyConfig != null)
        {
            client.setProxyConfig(proxyConfig);
        }

        try
        {
            int uriPort = endpointURI.getPort();
            if (uriPort != -1)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Using port: " + uriPort);
                }
                client.setPort(uriPort);
            }

            client.setConnectionTimeoutMillis(endpoint.getResponseTimeout());

            SftpUtil sftpUtil = new SftpUtil(endpoint);
            String identityFile = sftpUtil.getIdentityFile();
            if (sftpUtil.getKnownHostsFile() != null)
            {
                client.setKnownHostsFile(new File(sftpUtil.getKnownHostsFile()));
            }

            /*
             * TODO: There is a problem if the SSHd uses a low value of
             * "MaxStartups", which means that if there is many new concurrent
             * connections the server will respond with "Connection reset", and thus
             * we will get exceptions of type
             * "Session.connect: java.net.SocketException: Connection reset"...
             * Solutions: increase the MaxStartups on the server or fix a retry or
             * use a exception-strategy(?)
             */

            // boolean succeeded = false;
            // int numberOfAttempts = 2;
            // while(!succeeded && numberOfAttempts > 0)
            // {
            // try
            // {
            if (identityFile != null)
            {
                String passphrase = sftpUtil.getPassphrase();

                client.login(endpointURI.getUser(), identityFile, passphrase);
            }
            else
            {
                client.login(endpointURI.getUser(), endpointURI.getPassword());
            }
            // } catch (SocketException e)
            // {
            // numberOfAttempts--;
            // continue;
            // }
            // succeeded = true;
            // }

            if (logger.isDebugEnabled())
            {
                logger.debug("Successfully connected to: " + endpointURI);
            }

            return client;
        }
        catch (IOException e)
        {
            client.disconnect();
            throw e;
        }
    }

    @Override
    public void passivateObject(Object o) throws Exception
    {
        // Do nothing!
    }

    @Override
    public boolean validateObject(Object o)
    {
        SftpClient client = (SftpClient) o;
        if (logger.isDebugEnabled())
        {
            logger.debug("Inside validateObject - will return " + client.isConnected());
        }
        return client.isConnected();
    }

    public void setPreferredAuthenticationMethods(String preferredAuthenticationMethods)
    {
        this.preferredAuthenticationMethods = preferredAuthenticationMethods;
    }

    public void setProxyConfig(SftpProxyConfig proxyConfig)
    {
        this.proxyConfig = proxyConfig;
    }
}
