/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.impl.StaticMDCBinder;
import org.slf4j.spi.MDCAdapter;
import uk.org.lidalia.lang.ThreadLocal;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestMDCAdapter;

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

  @After
  public void clearMDCThreadReferences() throws NoSuchFieldException, IllegalAccessException {
    // TestMDCAdapter contains ThreadLocal variables with strong references to Threads which should be released to prevent
    // possible leakages
    MDCAdapter testMDCAdapter = StaticMDCBinder.SINGLETON.getMDCA();
    Field valueField = TestMDCAdapter.class.getDeclaredField("value");
    valueField.setAccessible(true);
    ThreadLocal<Map<String, String>> threadLocal = (ThreadLocal<Map<String, String>>) valueField.get(testMDCAdapter);
    threadLocal.reset();
  }
}
