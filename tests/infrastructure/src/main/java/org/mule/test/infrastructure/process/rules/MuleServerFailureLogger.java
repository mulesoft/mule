/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import org.mule.test.infrastructure.process.MuleProcessController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This rules logs Mule server logs content in case of a test failure.
 * <p>
 * 
 * <pre>
 * public class MuleServerTestCase {
 *
 *   protected MuleInstallation installation = new MuleInstallation(getProperty("distribution"));
 *   &#064;Rule
 *   public TestRule chain = outerRule(installation).around(new MuleServerFailureLogger(installation));
 *
 *   &#064;Test
 *   public void testMuleServer() throws IOException {
 *     // Start Mule
 *     // This code exercises Mule server
 *   }
 * }
 * </pre>
 *
 * 
 */
public class MuleServerFailureLogger extends TestWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleServerFailureLogger.class);
  private final MuleInstallation installation;

  public MuleServerFailureLogger(MuleInstallation installation) {
    this.installation = installation;
  }

  @Override
  protected void failed(Throwable e, Description description) {
    String serverLog = new MuleProcessController(installation.getMuleHome()).getLog().getAbsolutePath();
    try (Stream<String> stream = Files.lines(Paths.get(serverLog))) {
      LOGGER.error("====================== Server log ===============================");
      stream.forEach(LOGGER::error);
      LOGGER.error("=================================================================");
    } catch (IOException e1) {
      LOGGER.error("Failed to log server log");
    }
  }
}
