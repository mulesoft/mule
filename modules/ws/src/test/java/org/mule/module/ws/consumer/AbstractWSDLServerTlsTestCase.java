/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.junit.rules.ExternalResource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.junit.ClassRule;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This tests mocks a proxy server through which a wsdl file is served.
 */
public class AbstractWSDLServerTlsTestCase extends FunctionalTestCase
{

    @ClassRule
    public static DynamicPort port = new DynamicPort("port");

    private static final String WSDL_FILE_LOCATION = "/Test.wsdl";

    @ClassRule
    public static ExternalResource myServer = new ServerResource(port);

    /**
     * JUnit rule to initialize and teardown the HTTPS server
     */
    public static class ServerResource extends ExternalResource
    {
        private Server server;
        private DynamicPort port;

        ServerResource(DynamicPort port)
        {
            this.port = port;
        }

        @Override
        protected void before() throws Throwable
        {
            server = new Server(port.getNumber());
            server.start();
        }

        @Override
        protected void after()
        {
            try
            {
                server.stop();
            }
            catch (Exception e)
            {
                throw new RuntimeException("server stop failed");
            }
        }
    }

    /**
     * Embedded HTTPS server that returns a WSDL
     */
    public static class Server
    {
        HttpServer webServer;
        int port;

        SSLEngineConfigurator sslServerEngineConfig;

        public Server(int port)
        {
            this.port = port;
        }

        protected void start() throws IOException
        {
            NetworkListener networkListener = new NetworkListener("sample-listener", "localhost", port);

            sslServerEngineConfig = new SSLEngineConfigurator(createSSLContextConfigurator().createSSLContext(), false, false, false);
            networkListener.setSSLEngineConfig(sslServerEngineConfig);

            webServer = HttpServer.createSimpleServer();
            webServer.addListener(networkListener);

            webServer.getServerConfiguration().addHttpHandler(new HttpHandler()
            {
                public void service(Request request, Response response) throws Exception
                {
                    response.setContentType(APPLICATION_XML_UTF_8.toString());
                    final InputStream wsdlStream = this.getClass().getResourceAsStream(WSDL_FILE_LOCATION);
                    final String contents = IOUtils.toString(wsdlStream, UTF_8.name());
                    response.setContentLength(contents.length());
                    response.getWriter().write(contents);
                }
            });

            networkListener.setSecure(true);
            webServer.start();
        }

        protected void stop()
        {
            webServer.shutdownNow();
        }

        private SSLContextConfigurator createSSLContextConfigurator()
        {
            SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
            ClassLoader cl = AbstractWSDLServerTlsTestCase.class.getClassLoader();

            URL cacertsUrl = cl.getResource("trustStore");
            if (cacertsUrl != null)
            {
                sslContextConfigurator.setTrustStoreFile(cacertsUrl.getFile());
                sslContextConfigurator.setTrustStorePass("mulepassword");
            }

            URL keystoreUrl = cl.getResource("serverKeystore");
            if (keystoreUrl != null)
            {
                sslContextConfigurator.setKeyStoreFile(keystoreUrl.getFile());
                sslContextConfigurator.setKeyStorePass("mulepassword");
                sslContextConfigurator.setKeyPass("mulepassword");
            }

            return sslContextConfigurator;
        }

    }

}
