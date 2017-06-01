/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.tls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.security.tls.TlsConfiguration;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultTlsContextFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void createTlsPropertiesFile() throws Exception {

    // TODO MULE-10805: It's ignored when Allure is used because it fails with the Allure Surefire listener
    assumeThat(System.getProperty("allure.profile.is.activated", "false"), Is.is(equalTo("false")));

    PrintWriter writer = new PrintWriter(getTlsPropertiesFile(), "UTF-8");
    writer.println("enabledCipherSuites=" + getFileEnabledCipherSuites());
    writer.println("enabledProtocols=" + getFileEnabledProtocols());
    writer.close();
  }

  @AfterClass
  public static void removeTlsPropertiesFile() {
    getTlsPropertiesFile().delete();
  }

  private static File getTlsPropertiesFile() {
    String path = ClassUtils.getClassPathRoot(DefaultTlsContextFactoryTestCase.class).getPath();
    return new File(path, String.format(TlsConfiguration.PROPERTIES_FILE_PATTERN, TlsConfiguration.DEFAULT_SECURITY_MODEL));
  }

  public static String getFileEnabledProtocols() {
    return "TLSv1.1, TLSv1.2";
  }

  public static String getFileEnabledCipherSuites() {
    return "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, TLS_DHE_DSS_WITH_AES_128_CBC_SHA";
  }

  @Test
  public void failIfTrustStoreIsNonexistent() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    expectedException.expect(IOException.class);
    expectedException.expectMessage(containsString("Resource non-existent-trust-store could not be found"));
    tlsContextFactory.setTrustStorePath("non-existent-trust-store");
  }

  @Test
  public void useConfigFileIfDefaultProtocolsAndCipherSuites() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setEnabledCipherSuites("DEFAULT");
    tlsContextFactory.setEnabledProtocols("default");
    tlsContextFactory.initialise();

    assertThat(tlsContextFactory.getEnabledCipherSuites(), is(StringUtils.splitAndTrim(getFileEnabledCipherSuites(), ",")));
    assertThat(tlsContextFactory.getEnabledProtocols(), is(StringUtils.splitAndTrim(getFileEnabledProtocols(), ",")));
  }

  @Test
  public void overrideConfigFile() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setEnabledCipherSuites("TLS_DHE_DSS_WITH_AES_128_CBC_SHA");
    tlsContextFactory.setEnabledProtocols("TLSv1.1");
    tlsContextFactory.initialise();

    String[] enabledCipherSuites = tlsContextFactory.getEnabledCipherSuites();
    assertThat(enabledCipherSuites.length, is(1));
    assertThat(enabledCipherSuites, is(arrayContaining("TLS_DHE_DSS_WITH_AES_128_CBC_SHA")));

    String[] enabledProtocols = tlsContextFactory.getEnabledProtocols();
    assertThat(enabledProtocols.length, is(1));
    assertThat(enabledProtocols, is(arrayContaining("TLSv1.1")));
  }

  @Test
  public void failIfProtocolsDoNotMatchConfigFile() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setEnabledProtocols("TLSv1,SSLv3");
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("protocols are invalid"));
    tlsContextFactory.initialise();
  }

  @Test
  public void failIfCipherSuitesDoNotMatchConfigFile() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    tlsContextFactory.setEnabledCipherSuites("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA");
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("cipher suites are invalid"));
    tlsContextFactory.initialise();
  }

  @Test
  public void cannotMutateEnabledProtocols() throws InitialisationException {
    TlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    initialiseIfNeeded(tlsContextFactory);
    tlsContextFactory.getEnabledProtocols()[0] = "TLSv1";
    assertThat(tlsContextFactory.getEnabledProtocols(), arrayWithSize(2));
    assertThat(tlsContextFactory.getEnabledProtocols(), arrayContaining("TLSv1.1", "TLSv1.2"));
  }

  @Test
  public void cannotMutateEnabledCipherSuites() throws InitialisationException {
    TlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    initialiseIfNeeded(tlsContextFactory);
    tlsContextFactory.getEnabledCipherSuites()[0] = "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256";
    assertThat(tlsContextFactory.getEnabledCipherSuites(), arrayWithSize(2));
    assertThat(tlsContextFactory.getEnabledCipherSuites(), arrayContaining("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
                                                                           "TLS_DHE_DSS_WITH_AES_128_CBC_SHA"));
  }

}
