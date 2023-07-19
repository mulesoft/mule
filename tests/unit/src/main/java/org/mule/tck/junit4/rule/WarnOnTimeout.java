/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.junit4.rule;

import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a {@link Statement} to execute a test with a given timeout. Differently from JUnit's
 * {@link org.junit.internal.runners.statements.FailOnTimeout} this statement just prints a warning in the log, so the test will
 * pass in case of timeout.
 */
public class WarnOnTimeout extends Statement {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Statement next;
  private final long timeout;
  private boolean finished = false;
  private Throwable thrown = null;

  public WarnOnTimeout(Statement next, long timeout) {
    this.next = next;
    this.timeout = timeout;
  }

  @Override
  public void evaluate() throws Throwable {
    Thread thread = new Thread() {

      @Override
      public void run() {
        try {
          next.evaluate();
          finished = true;
        } catch (Throwable e) {
          thrown = e;
        }
      }
    };
    thread.start();
    thread.join(timeout);
    if (finished) {
      return;
    }
    if (thrown != null) {
      throw thrown;
    }

    logger.warn("Timeout of " + timeout + "ms exceeded");
  }
}
