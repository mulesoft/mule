/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static java.lang.String.format;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.DEFAULT_KEYSTORE;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.DEFAULT_SECURITY_MODEL;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.DEFAULT_SSL_TYPE;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.JSSE_NAMESPACE;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.util.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Test;

public class TlsConfigurationTestCase extends AbstractMuleTestCase {

  private static final String SUPPORTED_CIPHER_SUITE = "TLS_DHE_DSS_WITH_AES_128_CBC_SHA";
  private static final String SUPPORTED_PROTOCOL = "TLSv1.1";
  private static final String TEST_SECURITY_MODEL = "test";

  @Test
  public void testEmptyConfiguration() throws Exception {
    TlsConfiguration configuration = new TlsConfiguration(DEFAULT_KEYSTORE);
    try {
      configuration.initialise(false, JSSE_NAMESPACE);
      fail("no key password");
    } catch (IllegalArgumentException e) {
      assertNotNull("expected", e);
    }
    configuration.setKeyPassword("mulepassword");
    try {
      configuration.initialise(false, JSSE_NAMESPACE);
      fail("no store password");
    } catch (IllegalArgumentException e) {
      assertNotNull("expected", e);
    }
    configuration.setKeyStorePassword("mulepassword");
    configuration.setKeyStore(""); // guaranteed to not exist
    try {
      configuration.initialise(false, JSSE_NAMESPACE);
      fail("no keystore");
    } catch (Exception e) {
      assertNotNull("expected", e);
    }
  }

  @Test
  public void testSimpleSocket() throws Exception {
    TlsConfiguration configuration = new TlsConfiguration(DEFAULT_KEYSTORE);
    configuration.setKeyPassword("mulepassword");
    configuration.setKeyStorePassword("mulepassword");
    configuration.setKeyStore("clientKeystore");
    configuration.initialise(false, JSSE_NAMESPACE);
    SSLSocketFactory socketFactory = configuration.getSocketFactory();
    assertTrue("socket is useless", socketFactory.getSupportedCipherSuites().length > 0);
  }

  @Test
  public void testExceptionOnInvalidKeyAlias() throws Exception {
    URL keystoreUrl = getClass().getClassLoader().getResource("serverKeystore");
    File keystoreFile = new File(keystoreUrl.toURI());

    TlsConfiguration config = new TlsConfiguration(keystoreFile.getAbsolutePath());
    config.setKeyStorePassword("mulepassword");
    config.setKeyPassword("mulepassword");
    config.setKeyAlias("this_key_does_not_exist_in_the_keystore");

    try {
      config.initialise(false, JSSE_NAMESPACE);
    } catch (CreateException ce) {
      assertTrue(ce.getCause() instanceof IllegalStateException);
    }
  }


  @Test
  public void testCipherSuitesFromConfigFile() throws Exception {
    File configFile = createDefaultConfigFile();

    try {
      TlsConfiguration tlsConfiguration = new TlsConfiguration(DEFAULT_KEYSTORE);
      tlsConfiguration.initialise(true, JSSE_NAMESPACE);

      SSLSocket socket = (SSLSocket) tlsConfiguration.getSocketFactory().createSocket();
      SSLServerSocket serverSocket = (SSLServerSocket) tlsConfiguration.getServerSocketFactory().createServerSocket();

      assertArrayEquals(new String[] {SUPPORTED_CIPHER_SUITE}, socket.getEnabledCipherSuites());
      assertArrayEquals(new String[] {SUPPORTED_CIPHER_SUITE}, serverSocket.getEnabledCipherSuites());
    } finally {
      configFile.delete();
    }
  }

  @Test
  public void testProtocolsFromConfigFile() throws Exception {
    File configFile = createDefaultConfigFile();

    try {
      TlsConfiguration tlsConfiguration = new TlsConfiguration(DEFAULT_KEYSTORE);
      tlsConfiguration.initialise(true, JSSE_NAMESPACE);

      SSLSocket socket = (SSLSocket) tlsConfiguration.getSocketFactory().createSocket();
      SSLServerSocket serverSocket = (SSLServerSocket) tlsConfiguration.getServerSocketFactory().createServerSocket();

      assertArrayEquals(new String[] {SUPPORTED_PROTOCOL}, socket.getEnabledProtocols());
      assertArrayEquals(new String[] {SUPPORTED_PROTOCOL}, serverSocket.getEnabledProtocols());
    } finally {
      configFile.delete();
    }
  }

  @Test
  public void defaultProtocol() throws Exception {
    TlsConfiguration tlsConfiguration = new TlsConfiguration(DEFAULT_KEYSTORE);
    tlsConfiguration.initialise(true, JSSE_NAMESPACE);

    SSLSocketFactory socketFactory = tlsConfiguration.getSocketFactory();
    SSLServerSocketFactory serverSocketFactory = tlsConfiguration.getServerSocketFactory();

    SSLContext sslContext = SSLContext.getInstance(DEFAULT_SSL_TYPE);
    sslContext.init(null, null, null);

    assertThat(socketFactory.getDefaultCipherSuites(),
               arrayContainingInAnyOrder(sslContext.getSocketFactory().getDefaultCipherSuites()));
  }

  @Test
  public void defaultProtocolFromConfigFile() throws Exception {
    File configFile = createDefaultProtocolConfigFile();

    try {
      TlsConfiguration tlsConfiguration = new TlsConfiguration(DEFAULT_KEYSTORE);
      tlsConfiguration.initialise(true, JSSE_NAMESPACE);

      SSLSocketFactory socketFactory = tlsConfiguration.getSocketFactory();
      SSLServerSocketFactory serverSocketFactory = tlsConfiguration.getServerSocketFactory();

      SSLContext sslContext = SSLContext.getInstance(SUPPORTED_PROTOCOL);
      sslContext.init(null, null, null);

      SSLSocketFactory protocolSocketFactory = sslContext.getSocketFactory();
      SSLServerSocketFactory protocolServerSocketFactory = sslContext.getServerSocketFactory();

      assertThat(socketFactory.getDefaultCipherSuites(), arrayWithSize(protocolSocketFactory.getDefaultCipherSuites().length));
      assertThat(socketFactory.getDefaultCipherSuites(),
                 is(arrayContainingInAnyOrder(protocolSocketFactory.getDefaultCipherSuites())));
      assertThat(serverSocketFactory.getDefaultCipherSuites(),
                 arrayWithSize(protocolServerSocketFactory.getDefaultCipherSuites().length));
      assertThat(serverSocketFactory.getDefaultCipherSuites(),
                 is(arrayContainingInAnyOrder(protocolServerSocketFactory.getDefaultCipherSuites())));
    } finally {
      configFile.delete();
    }
  }

  @Test
  public void overrideDefaultProtocolFromConfigFile() throws Exception {
    File configFile = createDefaultProtocolConfigFile();

    try {
      TlsConfiguration tlsConfiguration = new TlsConfiguration(DEFAULT_KEYSTORE);
      tlsConfiguration.setSslType("TLSv1.2");
      tlsConfiguration.initialise(true, JSSE_NAMESPACE);

      SSLSocketFactory socketFactory = tlsConfiguration.getSocketFactory();

      SSLContext sslContext = SSLContext.getInstance(SUPPORTED_PROTOCOL);
      sslContext.init(null, null, null);

      SSLSocketFactory protocolSocketFactory = sslContext.getSocketFactory();

      assertThat(socketFactory.getDefaultCipherSuites(),
                 not(arrayWithSize(protocolSocketFactory.getDefaultCipherSuites().length)));
    } finally {
      configFile.delete();
    }
  }


  @Test
  public void testSecurityModelProperty() throws Exception {
    String previousSecurityModel = SecurityUtils.getSecurityModel();
    System.setProperty(MuleProperties.MULE_SECURITY_SYSTEM_PROPERTY, TEST_SECURITY_MODEL);
    File file = createConfigFile(TEST_SECURITY_MODEL, "enabledCipherSuites=TEST");

    try {
      TlsConfiguration tlsConfiguration = new TlsConfiguration(DEFAULT_KEYSTORE);
      tlsConfiguration.initialise(true, JSSE_NAMESPACE);

      assertArrayEquals(new String[] {"TEST"}, tlsConfiguration.getEnabledCipherSuites());
    } finally {
      System.setProperty(MuleProperties.MULE_SECURITY_SYSTEM_PROPERTY, previousSecurityModel);
      file.delete();
    }
  }

  private File createDefaultProtocolConfigFile() throws IOException {
    return createConfigFile(DEFAULT_SECURITY_MODEL, format("defaultProtocol=%s", SUPPORTED_PROTOCOL));
  }

  private File createDefaultConfigFile() throws IOException {
    String contents = String.format("enabledCipherSuites=UNSUPPORTED,%s\n" + "enabledProtocols=UNSUPPORTED,%s",
                                    SUPPORTED_CIPHER_SUITE, SUPPORTED_PROTOCOL);

    return createConfigFile(DEFAULT_SECURITY_MODEL, contents);
  }

  private File createConfigFile(String securityModel, String contents) throws IOException {
    String path = ClassUtils.getClassPathRoot(getClass()).getPath();
    File file = new File(path, String.format(TlsConfiguration.PROPERTIES_FILE_PATTERN, securityModel));

    PrintWriter writer = new PrintWriter(file, "UTF-8");
    writer.println(contents);
    writer.close();

    return file;
  }
}
