/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a process that starts a server required by the test.
 *
 * @since 4.2
 */
public final class ExternalProcess extends ExternalResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalProcess.class);

  private final Predicate<String> daemonStartedPredicate;
  private final String[] processCommand;

  private Process p;
  private ExecutorService daemonExecutor;
  private Future<?> daemonFuture;

  public ExternalProcess(Predicate<String> daemonStartedPredicate, String... processCommand) {
    this.daemonStartedPredicate = daemonStartedPredicate;
    this.processCommand = processCommand;
  }

  @Override
  protected void before() throws Throwable {
    super.before();

    LOGGER.trace("Starting external process: " + asList(processCommand));

    ProcessBuilder pb = new ProcessBuilder(processCommand);
    pb.redirectErrorStream(true);

    AtomicBoolean started = new AtomicBoolean();

    daemonExecutor = newSingleThreadExecutor();
    daemonFuture = daemonExecutor.submit(() -> {
      try {
        p = pb.start();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {

          LOGGER.warn(line);
          if (daemonStartedPredicate.test(line)) {
            Thread.sleep(500);
            started.set(true);
          }
          line = reader.readLine();
        }
      } catch (IOException | InterruptedException e) {
        if (p.isAlive()) {
          LOGGER.error("Exception reading process output", e);
        }
        return;
      }
    });

    while (!started.get()) {
      Thread.sleep(10);
    }
  }

  @Override
  protected void after() {
    try {
      p.destroy();
      daemonFuture.get(30, SECONDS);
      daemonExecutor.shutdown();

      while (p.isAlive()) {
        Thread.sleep(10);
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }

    super.after();
  }
}
