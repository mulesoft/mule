/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.FtpTestHarness.HELLO_WORLD;

import org.mule.extension.FtpTestHarness;
import org.mule.extension.ftp.AbstractFtpConnectorTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class SftpProxyTestCase extends AbstractFtpConnectorTestCase {

  @Rule
  public final FtpTestHarness testHarness = new SftpTestHarness();

  @Rule
  public DynamicPort proxyPort = new DynamicPort("proxyPort");

  TestProxyServer proxyServer;

  public SftpProxyTestCase() throws Exception {
    proxyServer = new TestProxyServer(proxyPort.getNumber(), testHarness.getServerPort());
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    proxyServer.start();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    proxyServer.stop();
    super.doTearDownAfterMuleContextDispose();
  }

  @Override
  protected String getConfigFile() {
    return "sftp-proxy-config.xml";
  }

  @Test
  public void connectsThroughProxy() throws Exception {
    testHarness.createHelloWorldFile();
    Message message = flowRunner("sftpRead").run().getMessage();
    assertThat(getPayloadAsString(message), is(HELLO_WORLD));
  }
}
