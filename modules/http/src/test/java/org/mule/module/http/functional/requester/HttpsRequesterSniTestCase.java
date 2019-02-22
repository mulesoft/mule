/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.util.FileUtils.deleteFile;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.sni.SNIConfig;
import org.glassfish.grizzly.sni.SNIFilter;
import org.glassfish.grizzly.sni.SNIServerConfigResolver;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class HttpsRequesterSniTestCase extends FunctionalTestCase
{
    private static final String FQDN = "localhost.localdomain";

    private static final String SERVER_PROTOCOL_ENABLED = "SSLv3,TLSv1,TLSv1.1,TLSv1.2";

    @Rule
    public DynamicPort httpsPort = new DynamicPort("httpsPort");

    private Server server;

    @Override
    protected String getConfigFile()
    {
        return "http-request-sni-config.xml";
    }

    @BeforeClass
    public static void createTlsPropertiesFile() throws Exception
    {
        PrintWriter writer = new PrintWriter(getTlsPropertiesFile(), "UTF-8");
        writer.println("enabledProtocols=" + SERVER_PROTOCOL_ENABLED);
        writer.close();
    }

    @AfterClass
    public static void removeTlsPropertiesFile()
    {
        deleteFile(getTlsPropertiesFile());
    }

    private static File getTlsPropertiesFile()
    {
        String path = System.getProperty("testClasspathDir");
        if (path == null)
        {
            path = ClassUtils.getClassPathRoot(HttpsRequesterSniTestCase.class).getPath();
        }
        return new File(path, String.format(TlsConfiguration.PROPERTIES_FILE_PATTERN, TlsConfiguration.DEFAULT_SECURITY_MODEL));
    }

    @Before
    public void prepareServer() throws IOException
    {
        server = new Server(httpsPort.getNumber());
        server.startServer();
    }

    @After
    public void teardownServer() {
        if (server != null)
        {
            server.stopServer();
        }
    }

    @Test(expected = MessagingException.class)
    public void testClientSNINotSentOnNonFQDN() throws Exception
    {
        runFlow("requestFlowLocalhost", getTestEvent(TEST_MESSAGE));
    }

    /*
     * SNI requires a fully qualified domain name. "localhost.localdomain" is used but it is not commonly present
     * on MacOSX hosts. An assumption will prevent its execution unless the domain exists. Although is recommended to
     * add the aforementioned domain to the /etc/ file if it's not present.
     */
    @Test
    public void testClientSNISentOnFQDN() throws Exception
    {
        InetAddress address = null;
        try
        {
            address = InetAddress.getByName(FQDN);
        }
        catch (Exception e) {}

        assumeThat(address, is(notNullValue()));

        runFlow("requestFlowFQDN", getTestEvent(TEST_MESSAGE));
        assertThat(server.getHostname(), is(FQDN));
    }

    /**
     * Executes the given flow with the given {@code event}
     *
     * @param flowName the name of the flow to be executed
     * @param event the event which we'll use to execute the flow
     * @return the resulting <code>MuleEvent</code>
     * @throws Exception
     */
    @Override
    protected MuleEvent runFlow(String flowName, MuleEvent event) throws Exception
    {
        Flow flow = lookupFlowConstruct(flowName);
        return flow.process(event);
    }

    /**
     * Embedded HTTPS server that fails to serve if SNI extension is not honored
     */
    public class Server
    {
        HttpServer webServer;
        final AtomicReference<String> sniHostname;
        int port;

        SSLEngineConfigurator sslServerEngineConfig;

        public Server(int port)
        {
            sniHostname = new AtomicReference<String>();
            this.port = port;
        }

        protected void startServer() throws IOException
        {
            NetworkListener networkListener = new NetworkListener("sample-listener", "localhost", port);

            sslServerEngineConfig = new SSLEngineConfigurator(createSSLContextConfigurator().createSSLContext(), false, false, false);
            networkListener.setSSLEngineConfig(sslServerEngineConfig);

            webServer = HttpServer.createSimpleServer();
            webServer.addListener(networkListener);
            networkListener.setSecure(true);
            networkListener.registerAddOn(new SniAddOn());
            webServer.start();
        }

        protected void stopServer()
        {
            sniHostname.set(StringUtils.EMPTY);
            webServer.shutdownNow();
        }

        private SNIFilter getSniFilter()
        {
            final Attribute<String> sniHostAttr = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("sni-host-attr");

            SNIFilter sniFilter = new SNIFilter();
            sniFilter.setServerSSLConfigResolver(new SNIServerConfigResolver()
            {
                @Override
                public SNIConfig resolve(Connection connection, String hostname)
                {
                    sniHostAttr.set(connection, hostname);
                    sniHostname.set(hostname);
                    if (StringUtils.isEmpty(hostname))
                    {
                        throw new IllegalArgumentException("SNI Has not been sent");
                    }
                    return SNIConfig.newServerConfig(sslServerEngineConfig);
                }
            });
            return sniFilter;
        }

        private SSLContextConfigurator createSSLContextConfigurator()
        {
            SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
            ClassLoader cl = HttpsRequesterSniTestCase.class.getClassLoader();

            URL cacertsUrl = cl.getResource("tls/sni-server-truststore.jks");
            if (cacertsUrl != null)
            {
                sslContextConfigurator.setTrustStoreFile(cacertsUrl.getFile());
                sslContextConfigurator.setTrustStorePass("changeit");
            }

            URL keystoreUrl = cl.getResource("tls/sni-server-keystore.jks");
            if (keystoreUrl != null)
            {
                sslContextConfigurator.setKeyStoreFile(keystoreUrl.getFile());
                sslContextConfigurator.setKeyStorePass("changeit");
                sslContextConfigurator.setKeyPass("changeit");
            }

            return sslContextConfigurator;
        }

        private class SniAddOn implements AddOn
        {
            @Override
            public void setup(NetworkListener networkListener, FilterChainBuilder builder)
            {
                // replace SSLFilter (if any) with SNIFilter
                final int idx = builder.indexOfType(SSLBaseFilter.class);
                if (idx != -1)
                {
                    builder.set(idx, getSniFilter());
                }
            }
        }

        public String getHostname()
        {
            return sniHostname.get();
        }
    }

}
