/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TestRule} which starts an apache {@link NetworkServerControl}.
 * <p>
 *
 * <pre>
 * public class UsingDerbyTestCase {
 *
 *   &#064;ClassRule
 *   public static final DerbyServer database = new DerbyServer(dbPort);
 *
 *   &#064;Test
 *   public void useDatabase() {
 *     // Here Database is up and running
 *     // You can get the connection URL:
 *     String url = database.getUri(name);
 *   }
 *
 *   &#064;Test
 *   public void reconnectionTestCase() {
 *     // You can stop and start again the database:
 *     database.stop();
 *     database.start();
 *   }
 * 
 * }
 * </pre>
 *
 */
public class DerbyServer extends ExternalResource implements Closeable {

  private static final String URI_TEMPLATE = "jdbc:derby://%s:%d/%s;create=true";
  private static final String DRIVER_CLASS = "org.apache.derby.jdbc.ClientDriver";
  private static final String DERBY_SYSTEM_HOME = "derby.system.home";
  private static final String DERBY_HOME = Paths.get(getProperty("user.dir")).resolve("derby.home").toString();
  private static final String HOST = "127.0.0.1";
  private static Logger logger = LoggerFactory.getLogger(DerbyServer.class);
  private NetworkServerControl server;
  private int port;

  public DerbyServer(int port) {
    setProperty(DERBY_SYSTEM_HOME, DERBY_HOME);
    this.port = port;
  }

  public String getUri(String name) {
    return String.format(URI_TEMPLATE, HOST, port, name);
  }

  public String getDriverClass() {
    return DRIVER_CLASS;
  }

  @Override
  public void before() {
    start();
  }

  @Override
  public void after() {
    stop();
  }

  public DerbyServer start() {
    try {
      server = new NetworkServerControl(InetAddress.getByName(HOST), port);
      server.start(new PrintWriter(new LogWriter()));
      await().until(() -> isRunning());
    } catch (Exception e) {
      throw new RuntimeException("Couldn't start Derby server", e);
    }
    logger.info("Started Derby Database server.");
    return this;
  }

  public boolean isRunning() {
    try {
      server.ping();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void stop() {
    try {
      server.shutdown();
      FileUtils.deleteQuietly(new File(DERBY_HOME));
      logger.info("Stopped Derby Database server.");
    } catch (Exception e) {
      logger.error("Failed to stop Database server: " + e.getMessage());
    }
  }

  @Override
  public void close() throws IOException {
    stop();
  }

  private static class LogWriter extends Writer {

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      logger.debug(new String(cbuf, off, len));
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
  }
}

