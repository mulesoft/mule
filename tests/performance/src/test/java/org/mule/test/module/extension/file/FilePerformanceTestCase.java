/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension.file;

import static org.junit.rules.ExpectedException.none;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.performance.util.AbstractIsolatedFunctionalPerformanceTestCase;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@Ignore("MULE-11450: Migrate Contiperf tests to JMH")
public class FilePerformanceTestCase extends AbstractIsolatedFunctionalPerformanceTestCase {

  private static final String HELLO_WORLD = "Hello World!";
  private static final String HELLO_FILE_NAME = "hello.json";
  private static final int REPETITIONS = 10;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public SystemProperty workingDir = new SystemProperty("workingDir", temporaryFolder.getRoot().getAbsolutePath());

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    if (!temporaryFolder.getRoot().exists()) {
      temporaryFolder.getRoot().mkdir();
    }
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    temporaryFolder.delete();
  }

  @Override
  protected String getConfigFile() {
    return "file-perf-test.xml";
  }

  @Before
  public void createFiles() throws IOException {
    createHelloWorldFile();
    createRandomFileOfSize("1K", 1024);
    createRandomFileOfSize("1M", 1024 * 1024);
  }

  @Test
  @Required(throughput = 110, average = 8, percentile90 = 10)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void readWrite() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("read-write").run();
    }
  }

  @Test
  @Required(throughput = 110, average = 8, percentile90 = 10)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void readWrite1K() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("read-write-1K").run();
    }
  }

  @Test
  @Required(throughput = 2, average = 350, percentile90 = 400)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void readWrite1M() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("read-write-1M").run();
    }
  }

  @Test
  @Required(throughput = 1000, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void read() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("read").run();
    }
  }

  @Test
  @Required(throughput = 1000, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void read1K() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("read-1K").run();
    }
  }

  @Test
  @Required(throughput = 1000, average = 1, percentile90 = 2)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void read1M() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("read-1M").run();
    }
  }

  @Test
  @Required(throughput = 120, average = 8, percentile90 = 10)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void write() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("write").run();
    }
  }

  @Test
  @Required(throughput = 110, average = 9, percentile90 = 11)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void writeBytearray() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("write-bytearray").run();
    }
  }

  @Test
  @Required(throughput = 120, average = 9, percentile90 = 11)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void writeIterable() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("write-iterable").run();
    }
  }

  @Test
  @Required(throughput = 110, average = 9, percentile90 = 11)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void writeLock() throws Exception {
    for (int i = 0; i < REPETITIONS; i++) {
      flowRunner("write-lock").run();
    }
  }


  protected File createHelloWorldFile() throws IOException {
    File folder = temporaryFolder.newFolder("files");
    File hello = new File(folder, HELLO_FILE_NAME);
    FileUtils.write(hello, HELLO_WORLD);

    return hello;
  }

  protected File createRandomFileOfSize(String folderName, int size) throws IOException {
    File folder = temporaryFolder.newFolder(folderName);
    File file = new File(folder, "file");
    FileUtils.write(file, RandomStringUtils.random(size));

    return file;
  }

}

