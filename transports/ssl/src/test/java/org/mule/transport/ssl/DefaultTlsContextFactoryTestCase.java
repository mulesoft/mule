/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.util.FileUtils.deleteFile;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultTlsContextFactoryTestCase extends AbstractMuleTestCase
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void createTlsPropertiesFile() throws Exception
    {
        PrintWriter writer = new PrintWriter(getTlsPropertiesFile(), "UTF-8");
        writer.println("enabledCipherSuites=" + getFileEnabledCipherSuites());
        writer.println("enabledProtocols=" + getFileEnabledProtocols());
        writer.close();
    }

    @AfterClass
    public static void removeTlsPropertiesFile()
    {
        deleteFile(getTlsPropertiesFile());
    }

    private static File getTlsPropertiesFile()
    {
        String path = ClassUtils.getClassPathRoot(DefaultTlsContextFactoryTestCase.class).getPath();
        return new File(path, String.format(TlsConfiguration.PROPERTIES_FILE_PATTERN, TlsConfiguration.DEFAULT_SECURITY_MODEL));
    }

    public static String getFileEnabledProtocols()
    {
        return "TLSv1.1, TLSv1.2";
    }

    public static String getFileEnabledCipherSuites()
    {
        return "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, TLS_DHE_DSS_WITH_AES_128_CBC_SHA";
    }

    @Test
    public void failIfKeyStoreHasNoKey() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setKeyStorePath("trustStore");
        tlsContextFactory.setKeyStorePassword("mulepassword");
        tlsContextFactory.setKeyManagerPassword("mulepassword");
        expectedException.expectCause(hasCause(isA(IllegalArgumentException.class)));
        expectedException.expectCause(hasCause(hasMessage(equalTo("No key entries found."))));
        tlsContextFactory.initialise();
    }

    @Test
    public void failIfKeyStoreAliasIsNotAKey() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setKeyStorePath("serverKeystore");
        tlsContextFactory.setKeyAlias("muleclient");
        tlsContextFactory.setKeyStorePassword("mulepassword");
        tlsContextFactory.setKeyManagerPassword("mulepassword");
        expectedException.expectCause(hasCause(isA(IllegalArgumentException.class)));
        expectedException.expectCause(hasCause(hasMessage(equalTo("Keystore entry for alias 'muleclient' is not a key."))));
        tlsContextFactory.initialise();
    }

    @Test
    public void failIfTrustStoreIsNonexistent() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        expectedException.expect(IOException.class);
        expectedException.expectMessage(containsString("Resource non-existent-trust-store could not be found"));
        tlsContextFactory.setTrustStorePath("non-existent-trust-store");
    }

    @Test
    public void useConfigFileIfDefaultProtocolsAndCipherSuites() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setEnabledCipherSuites("DEFAULT");
        tlsContextFactory.setEnabledProtocols("default");
        tlsContextFactory.initialise();

        assertThat(tlsContextFactory.getEnabledCipherSuites(), is(StringUtils.splitAndTrim(getFileEnabledCipherSuites(), ",")));
        assertThat(tlsContextFactory.getEnabledProtocols(), is(StringUtils.splitAndTrim(getFileEnabledProtocols(), ",")));
    }
    
    @Test
    public void trustStoreAlgorithmInTlsContextIsDefaultTrustManagerAlgorithm()
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        assertThat(tlsContextFactory.getTrustManagerAlgorithm(), equalTo(TrustManagerFactory.getDefaultAlgorithm()));
    }

    @Test
    public void overrideConfigFile() throws Exception
    {
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
    public void failIfProtocolsDoNotMatchConfigFile() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setEnabledProtocols("TLSv1,SSLv3");
        expectedException.expect(InitialisationException.class);
        expectedException.expectMessage(containsString("protocols are invalid"));
        tlsContextFactory.initialise();
    }

    @Test
    public void failIfCipherSuitesDoNotMatchConfigFile() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setEnabledCipherSuites("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA");
        expectedException.expect(InitialisationException.class);
        expectedException.expectMessage(containsString("cipher suites are invalid"));
        tlsContextFactory.initialise();
    }

    @Test
    public void defaultIncludesTls12Ciphers() throws Exception {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.initialise();
        SSLSocketFactory defaultFactory = tlsContextFactory.createSslContext().getSocketFactory();
        SSLContext tls12Context = SSLContext.getInstance("TLSv1.2");
        tls12Context.init(null, null, null);
        SSLSocketFactory tls12Factory = tls12Context.getSocketFactory();

        assertThat(defaultFactory.getDefaultCipherSuites(), arrayContainingInAnyOrder(tls12Factory.getDefaultCipherSuites()));
    }
}
