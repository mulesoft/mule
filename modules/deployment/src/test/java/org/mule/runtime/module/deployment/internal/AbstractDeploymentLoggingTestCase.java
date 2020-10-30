/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static uk.org.lidalia.slf4jtest.TestLoggerFactory.getInstance;
import static uk.org.lidalia.slf4jext.Level.INFO;

import org.junit.Before;
import org.junit.Rule;
import org.mule.tck.junit4.AbstractMuleTestCase;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

/**
 * Base class for deployment tests.
 * <p>
 * Provides a {@link Rule} to clean log message 
 */
public abstract class AbstractDeploymentLoggingTestCase extends AbstractMuleTestCase {

  @Rule
  public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

  @Before
  public void setupLogger() {
    getInstance().setPrintLevel(INFO);
  }
}
