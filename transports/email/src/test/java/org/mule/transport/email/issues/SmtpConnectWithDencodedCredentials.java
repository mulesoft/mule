/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.issues;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.Properties;

import javax.mail.Transport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.email.DefaultTransportConnector;
import org.mule.transport.email.TransportConnector;

/**
 * This test verifies that the credentials are already decoded when connecting to an SMTP server.
 */
@RunWith(MockitoJUnitRunner.class)
public class SmtpConnectWithDencodedCredentials
{

    private static final String PASSWORD = "password";
    private static final int PORT = 8080;
    private static final String HOST = "host";
    private static final String ENCODED_USER = "anypoint.store%40gmail.com";
    private static final String DECODED_USER = "anypoint.store@gmail.com";

    @Mock
    private Transport transport;

    @Test
    public void verifyDecodedCredentialsOnReconnect() throws Exception
    {
        TransportConnector connector = new DefaultTransportConnector(transport, UTF_8.name());
        EndpointURI uri = new TestEndpointURI();
        connector.connect(uri);
        verify(transport).connect(HOST, PORT, DECODED_USER, PASSWORD);
    }

    private static class TestEndpointURI implements EndpointURI
    {


        @Override
        public void initialise() throws InitialisationException
        {

        }

        @Override
        public String getAddress()
        {
            return null;
        }

        @Override
        public String getFilterAddress()
        {
            return null;
        }

        @Override
        public String getEndpointName()
        {
            return null;
        }

        @Override
        public Properties getParams()
        {
            return null;
        }

        @Override
        public Properties getUserParams()
        {
            return null;
        }

        @Override
        public String getScheme()
        {
            return null;
        }

        @Override
        public String getSchemeMetaInfo()
        {
            return null;
        }

        @Override
        public String getFullScheme()
        {
            return null;
        }

        @Override
        public String getAuthority()
        {
            return null;
        }

        @Override
        public String getHost()
        {
            return HOST;
        }

        @Override
        public int getPort()
        {
            return PORT;
        }

        @Override
        public String getPath()
        {
            return null;
        }

        @Override
        public String getQuery()
        {
            return null;
        }

        @Override
        public String getUserInfo()
        {
            return null;
        }

        @Override
        public String getTransformers()
        {
            return null;
        }

        @Override
        public String getResponseTransformers()
        {
            return null;
        }

        @Override
        public URI getUri()
        {
            return null;
        }

        @Override
        public String getConnectorName()
        {
            return null;
        }

        @Override
        public String getResourceInfo()
        {
            return null;
        }

        @Override
        public String getUser()
        {
            return ENCODED_USER;
        }

        @Override
        public String getPassword()
        {
            return PASSWORD;
        }

        @Override
        public MuleContext getMuleContext()
        {
            return null;
        }

    }
}
