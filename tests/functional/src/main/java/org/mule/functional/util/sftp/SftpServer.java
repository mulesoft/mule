/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util.sftp;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.security.Security;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SftpServer {

  public static final String USERNAME = "muletest1";
  public static final String PASSWORD = "muletest1";
  private SshServer sshdServer;
  private Integer port;

  public SftpServer(int port) {
    this.port = port;
    configureSecurityProvider();
    SftpSubsystemFactory factory = createFtpSubsystemFactory();
    sshdServer = SshServer.setUpDefaultServer();
    configureSshdServer(factory, passwordAuthenticator());
  }

  public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
    sshdServer.setPasswordAuthenticator(passwordAuthenticator);
  }

  private void configureSshdServer(SftpSubsystemFactory factory, PasswordAuthenticator passwordAuthenticator) {
    sshdServer.setPort(port);
    sshdServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser")));
    sshdServer.setSubsystemFactories(asList(factory));
    sshdServer.setCommandFactory(new ScpCommandFactory());
    sshdServer.setShellFactory(new ProcessShellFactory());
    sshdServer.setPasswordAuthenticator(passwordAuthenticator);
  }

  private SftpSubsystemFactory createFtpSubsystemFactory() {
    return new SftpSubsystemFactory();
  }

  private void configureSecurityProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  private static PasswordAuthenticator passwordAuthenticator() {
    return (arg0, arg1, arg2) -> USERNAME.equals(arg0) && PASSWORD.equals(arg1);
  }

  public void start() {
    try {
      sshdServer.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    try {
      sshdServer.stop(false);
    } catch (IOException e) {
      e.printStackTrace();
    }
    sshdServer = null;
  }
}
