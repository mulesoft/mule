/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.tls;

import static java.util.Collections.emptyMap;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;
import static org.apache.commons.lang3.SystemUtils.IS_JAVA_1_8;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.DEFAULT_SECURITY_MODEL;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.PROPERTIES_FILE_PATTERN;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultTlsContextFactoryTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void createTlsPropertiesFile() throws Exception {

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
    return new File(path, String.format(PROPERTIES_FILE_PATTERN, DEFAULT_SECURITY_MODEL));
  }

  public static String getFileEnabledProtocols() {
    return "TLSv1.1, TLSv1.2";
  }

  public static String getFileEnabledCipherSuites() {
    return "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, TLS_DHE_DSS_WITH_AES_128_CBC_SHA";
  }

  @Test
  public void insecureTrustStoreShouldNotBeConfigured() throws IOException, InitialisationException {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory(emptyMap(), getFeatureFlaggingService());
    tlsContextFactory.setTrustStorePath("trustStore");
    tlsContextFactory.setTrustStoreInsecure(true);
    assertFalse(tlsContextFactory.isTrustStoreConfigured());
  }

  private void defaultIncludesDEfaultTlsVersionCiphers(String sslVersion)
      throws InitialisationException, KeyManagementException, NoSuchAlgorithmException {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory(emptyMap());
    tlsContextFactory.initialise();
    SSLSocketFactory defaultFactory = tlsContextFactory.createSslContext().getSocketFactory();
    SSLContext tlsContext = SSLContext.getInstance(sslVersion);
    tlsContext.init(null, null, null);
    SSLSocketFactory tlsFactory = tlsContext.getSocketFactory();

    assertThat(defaultFactory.getDefaultCipherSuites(), arrayContainingInAnyOrder(tlsFactory.getDefaultCipherSuites()));
  }

}
