/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension;

import static org.junit.rules.ExpectedException.none;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.rules.ExpectedException;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Base class for {@link FtpTestHarness} implementations
 *
 * @since 4.0
 */
public abstract class AbstractFtpTestHarness extends ExternalResource implements FtpTestHarness {

  private SystemProperty profileSystemProperty;
  private ExpectedException expectedException = none();

  /**
   * Creates a new instance
   *
   * @param profile the name of a spring profile to activate
   */
  public AbstractFtpTestHarness(String profile) {
    profileSystemProperty = new SystemProperty("spring.profiles.active", profile);
  }

  @Override
  public final Statement apply(Statement base, Description description) {
    base = applyAll(base, description, profileSystemProperty, expectedException);
    base = applyAll(base, description, getChildRules());
    return super.apply(base, description);
  }

  /**
   * @return {@link TestRule testRules} declared on the implementations which should also be applied
   */
  protected abstract TestRule[] getChildRules();

  @Override
  protected final void before() throws Throwable {
    doBefore();
  }

  /**
   * Template method for performing setup actions
   */
  protected void doBefore() throws Throwable {

  }

  /**
   * Delegates into {@link #doAfter()} and resets the {@link #expectedException}
   */
  @Override
  protected final void after() {
    try {
      doAfter();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      expectedException = ExpectedException.none();
    }
  }

  /**
   * Template method for performing cleanup actions
   */
  protected void doAfter() throws Exception {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExpectedException expectedException() {
    return expectedException;
  }

  private Statement applyAll(Statement base, Description description, TestRule... rules) {
    for (TestRule rule : rules) {
      base = rule.apply(base, description);
    }

    return base;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(String folder, String fileName, String content) throws Exception {
    write(String.format("%s/%s", folder, fileName), content);
  }
}
