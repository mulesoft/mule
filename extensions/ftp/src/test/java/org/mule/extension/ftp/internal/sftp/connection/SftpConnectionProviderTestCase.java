/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.write;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.api.sftp.SftpAuthenticationMethod.GSSAPI_WITH_MIC;
import static org.mule.extension.ftp.internal.sftp.connection.SftpClient.PREFERRED_AUTHENTICATION_METHODS;
import static org.mule.extension.ftp.internal.sftp.connection.SftpClient.STRICT_HOST_KEY_CHECKING;
import static org.mule.functional.util.sftp.SftpServer.PASSWORD;
import static org.mule.functional.util.sftp.SftpServer.USERNAME;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableSet;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.File;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SftpConnectionProviderTestCase extends AbstractMuleTestCase {

  private static final String HOST = "localhost";
  private static final int TIMEOUT = 10;
  private static final String PASSPHRASE = "francis";

  private File hostFile;
  private File identityFile;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Mock
  private FtpConnector config;

  @Mock
  private JSch jsch;

  @Mock
  private Session session;

  @Mock
  private ChannelSftp channel;

  private SftpConnectionProvider provider = new SftpConnectionProvider();


  @Before
  public void before() throws Exception {
    hostFile = new File(folder.getRoot(), "host");
    identityFile = new File(folder.getRoot(), "identity");

    write(hostFile, "hostFile");
    write(identityFile, "jason bourne");

    provider.setHost(HOST);
    provider.setUsername(USERNAME);
    provider.setConnectionTimeout(10);
    provider.setConnectionTimeoutUnit(SECONDS);
    provider.setPreferredAuthenticationMethods(ImmutableSet.of(GSSAPI_WITH_MIC));
    provider.setKnownHostsFile(hostFile.getAbsolutePath());

    provider.setClientFactory(new SftpClientFactory() {

      @Override
      public SftpClient createInstance(String host, int port) {
        return new SftpClient(host, port, () -> jsch);
      }
    });

    when(jsch.getSession(USERNAME, HOST)).thenReturn(session);
    when(session.openChannel("sftp")).thenReturn(channel);
  }

  @Test
  public void identityFileWithPassPhrase() throws Exception {
    provider.setIdentityFile(identityFile.getAbsolutePath());
    provider.setPassphrase(PASSPHRASE);

    login();

    verify(jsch).addIdentity(identityFile.getAbsolutePath(), PASSPHRASE);
  }

  @Test
  public void identityFileWithoutPassPhrase() throws Exception {
    provider.setIdentityFile(identityFile.getAbsolutePath());

    login();

    assertSimpleIdentity();
  }

  private void assertSimpleIdentity() throws JSchException {
    verify(jsch).addIdentity(identityFile.getAbsolutePath());
  }

  @Test
  public void simpleCredentials() throws Exception {
    provider.setPassword(PASSWORD);
    login();

    assertPassword();
  }

  @Test
  public void simpleCredentialsPlusIdentity() throws Exception {
    provider.setIdentityFile(identityFile.getAbsolutePath());
    provider.setPassword(PASSWORD);

    login();

    assertPassword();
    assertSimpleIdentity();
  }

  @Test
  public void noKnownHosts() throws Exception {
    provider.setKnownHostsFile(null);
    provider.connect();

    Properties properties = captureLoginProperties();
    assertThat(properties.getProperty(STRICT_HOST_KEY_CHECKING), equalTo("no"));
  }

  private void assertPassword() {
    verify(session).setPassword(PASSWORD);
  }

  private void login() throws Exception {
    provider.connect();
    verify(jsch).setKnownHosts(hostFile.getAbsolutePath());
    verify(session).setTimeout(new Long(SECONDS.toMillis(TIMEOUT)).intValue());
    verify(session).connect();
    verify(channel).connect();

    Properties properties = captureLoginProperties();
    assertThat(properties.getProperty(PREFERRED_AUTHENTICATION_METHODS), equalTo(GSSAPI_WITH_MIC.toString()));
    assertThat(properties.getProperty(STRICT_HOST_KEY_CHECKING), equalTo("ask"));
  }

  private Properties captureLoginProperties() {
    ArgumentCaptor<Properties> propertiesCaptor = forClass(Properties.class);
    verify(session).setConfig(propertiesCaptor.capture());

    return propertiesCaptor.getValue();
  }
}
