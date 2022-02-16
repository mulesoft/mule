package org.mule.runtime.module.deployment.internal;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

/**
 * Abstract class for tests on the deployment module since SLF4J Test Logging Library is used
 * <p>
 * Configures loggers and clears logs
 */
public abstract class AbstractMuleWithTestLoggingSupportTestCase extends AbstractMuleTestCase {

  @Before
  public void clearAllLogs() {
    // Logs that are stored for later assert need to be cleared after every test
    // clearAll will reset state across all threads
    TestLoggerFactory.clearAll();
  }
}
