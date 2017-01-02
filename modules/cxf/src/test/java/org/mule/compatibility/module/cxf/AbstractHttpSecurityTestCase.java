/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.mule.extension.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.junit.Before;

public class AbstractHttpSecurityTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Before
  public void setUp() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setTrustStorePath("trustStore");
    tlsContextFactory.setTrustStorePassword("mulepassword");
    tlsContextFactory.initialise();

    SSLSocketFactory factory = tlsContextFactory.createSslContext().getSocketFactory();
    Protocol httpsWithTrustStore = new Protocol(HTTPS.getScheme(), getSocketFactory(factory), HTTPS.getDefaultPort());
    Protocol.registerProtocol(HTTPS.getScheme(), httpsWithTrustStore);
  }

  private static ProtocolSocketFactory getSocketFactory(final SSLSocketFactory factory) {
    return new ProtocolSocketFactory() {

      private SSLSocketFactory socketFactory = factory;

      @Override
      public Socket createSocket(String host, int port) throws IOException {
        return socketFactory.createSocket(host, port);
      }

      @Override
      public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return socketFactory.createSocket(host, port, localAddress, localPort);
      }

      @Override
      public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params)
          throws IOException {
        return createSocket(host, port, localAddress, localPort);
      }

    };
  }
}
