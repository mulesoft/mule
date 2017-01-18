/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension.ftp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.client.ftp.FTPTestClient;
import org.mule.test.infrastructure.process.rules.FtpServer;
import org.mule.test.performance.util.AbstractIsolatedFunctionalPerformanceTestCase;

import java.io.File;
import java.io.IOException;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-11450: Migrate Contiperf tests to JMH")
public class FtpPerformanceTestCase extends AbstractIsolatedFunctionalPerformanceTestCase {

  private static final String DEFAULT_FTP_HOST = "localhost";
  private static final String FTP_SERVER_BASE_DIR = "target/ftpserver";
  private static final String WORKING_DIR_SYSTEM_PROPERTY = "workingDir";
  private static final String WORKING_DIR = "base";
  protected static final File BASE_DIR = new File(FTP_SERVER_BASE_DIR, WORKING_DIR);
  private static final String FTP_USER = "anonymous";
  private static final String FTP_PASSWORD = "password";

  private FTPTestClient ftpClient;

  @Rule
  public SystemProperty workingDirSystemProperty = new SystemProperty(WORKING_DIR_SYSTEM_PROPERTY, WORKING_DIR);

  @Rule
  public FtpServer ftpServer = new FtpServer("ftpPort", BASE_DIR);

  @Override
  protected String getConfigFile() {
    return "ftp-perf-test.xml";
  }


  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();


    ftpServer.start();
    ftpClient = new FTPTestClient(DEFAULT_FTP_HOST, ftpServer.getPort(), FTP_USER, FTP_PASSWORD);

    if (!ftpClient.testConnection()) {
      throw new IOException("could not connect to ftp server");
    }
    ftpClient.changeWorkingDirectory(WORKING_DIR);

    ftpClient.putFile("copyMe.txt", "simpleContent");
    ftpClient.putFile("writeMe.txt", "simpleContent");



    assertThat(ftpClient.fileExists("copyMe.txt"), is(true));
    assertThat(ftpClient.fileExists("writeMe.txt"), is(true));
  }

  @Test
  @Required(throughput = 15, average = 55, percentile90 = 60)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void copy() throws Exception {
    flowRunner("copy").run();
  }

  @Test
  @Required(throughput = 8, average = 100, percentile90 = 110)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void writeAfterRead() throws Exception {
    flowRunner("read-and-write").run();
  }

  @Test
  @Required(throughput = 15, average = 55, percentile90 = 60)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void delete() throws Exception {
    ftpClient.putFile("delete.me", "I will be deleted.");
    flowRunner("delete").run();
  }

  @Test
  @Required(throughput = 20, average = 45, percentile90 = 50)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void list() throws Exception {
    flowRunner("list-with-embedded-predicate").run();
  }

  @Test
  @Required(throughput = 8, average = 95, percentile90 = 100)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void write() throws Exception {
    flowRunner("write").run();
    assertThat(ftpClient.fileExists("write.txt"), is(true));
  }


  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    super.doTearDownAfterMuleContextDispose();

    if (ftpClient != null) {
      try {
        if (ftpClient.isConnected()) {
          ftpClient.disconnect();
        }
      } finally {
        ftpServer.stop();
      }
    }
  }


}
