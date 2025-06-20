/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STREAMING_STATISTICS;
import static org.mule.runtime.api.util.MuleSystemProperties.TESTING_MODE_PROPERTY_NAME;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.core.api.util.SystemUtils.parsePropertyDefinitions;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.getAllStackTraces;
import static java.util.Collections.singletonMap;
import static java.util.Collections.sort;
import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;

import static org.junit.Assume.assumeThat;
import static org.junit.rules.Timeout.millis;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.api.util.SystemUtils;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.rule.LogCleanup;
import org.mule.tck.junit4.rule.WarningTimeout;
import org.mule.tck.report.ThreadDumpOnTimeOut;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.slf4j.Logger;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule test cases. This implementation provides services to test code for
 * creating mock and test objects.
 */
@Timeout(AbstractMuleTestCase.DEFAULT_TEST_TIMEOUT_SECS)
public abstract class AbstractMuleTestCase {

  public static final String TEST_PAYLOAD = "test";
  public static final String TEST_CONNECTOR = "test";

  public static final ComponentLocation TEST_CONNECTOR_LOCATION = from(TEST_CONNECTOR);

  public static final int DEFAULT_TEST_TIMEOUT_SECS = 60;

  public static final String TEST_TIMEOUT_SYSTEM_PROPERTY = "mule.test.timeoutSecs";

  /**
   * Indicates whether the text boxes will be logged when starting each test case.
   */
  private static final boolean verbose;

  static {
    String muleOpts = SystemUtils.getenv("MULE_TEST_OPTS");
    if (!isBlank(muleOpts)) {
      Map<String, String> parsedOpts = parsePropertyDefinitions(muleOpts);
      String optVerbose = parsedOpts.get("mule.verbose");
      verbose = valueOf(optVerbose);
    } else {
      verbose = true;
    }

    System.setProperty(TESTING_MODE_PROPERTY_NAME, "true");
    System.setProperty(MULE_ENABLE_STREAMING_STATISTICS, "true");
  }

  private static final Logger LOGGER = getLogger(AbstractMuleTestCase.class);

  /**
   * Should be set to a string message describing any prerequisites not met.
   */
  private final boolean offline = "true".equalsIgnoreCase(System.getProperty("org.mule.offline"));

  private final int testTimeoutSecs = getTimeoutSystemProperty();

  @Rule
  public TestName name = new TestName();

  @Rule
  public final TestRule chain = RuleChain
      .outerRule(new LogCleanup())
      .around(new ThreadDumpOnTimeOut())
      .around(createTestTimeoutRule());

  /**
   * Creates the timeout rule that will be used to run the test.
   *
   * @return the rule used to check for test execution timeouts.
   */
  protected TestRule createTestTimeoutRule() {
    int millisecondsTimeout = getTestTimeoutSecs() * 1000;

    if (isFailOnTimeout()) {
      return new DisableOnDebug(millis(millisecondsTimeout));
    } else {
      return new WarningTimeout(millisecondsTimeout);
    }
  }

  /**
   * Defines the number of seconds that a test has in order to run before throwing a timeout. If the property if not defined then
   * uses the <code>DEFAULT_MULE_TEST_TIMEOUT_SECS</code> constant.
   *
   * @return the timeout value expressed in seconds
   */
  protected int getTimeoutSystemProperty() {
    String timeoutString = System.getProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, null);
    if (timeoutString == null) {
      // unix style: MULE_TEST_TIMEOUTSECS
      String variableName = TEST_TIMEOUT_SYSTEM_PROPERTY.toUpperCase().replace(".", "_");
      timeoutString = System.getenv(variableName);
    }

    int result = DEFAULT_TEST_TIMEOUT_SECS;
    if (timeoutString != null) {
      try {
        result = Integer.parseInt(timeoutString);
      } catch (NumberFormatException e) {
        // Uses the default value
      }
    }

    return result;
  }

  /**
   * Subclasses can override this method to skip the execution of the entire test class.
   *
   * @return <code>true</code> if the test class should not be run.
   */
  protected boolean isDisabledInThisEnvironment() {
    return false;
  }

  /**
   * Should this test run?
   *
   * @param testMethodName name of the test method
   * @return whether the test should execute in the current environment
   */
  protected boolean isDisabledInThisEnvironment(String testMethodName) {
    return false;
  }

  public boolean isOffline(String method) {
    if (offline) {
      LOGGER.warn("Working offline cannot run test: " + method);
    }

    return offline;
  }

  /**
   * Defines the timeout in seconds that will be used to run the test.
   *
   * @return the timeout in seconds
   */
  public int getTestTimeoutSecs() {
    return testTimeoutSecs;
  }

  /**
   * Could be useful to use it in place of hardcoding the pom project version in the tests.
   *
   * @return the "project.version" maven property.
   */
  protected static String getMavenProjectVersionProperty() {
    return getProperty("maven.projectVersion");
  }

  @Before
  public final void initializeMuleTest() {
    skipTestWhenDisabledInCurrentEnvironment(name.getMethodName());
    printTestHeader();
  }

  private void printTestHeader() {
    if (verbose) {
      System.out.println(" => " + getTestHeader());
    }
  }

  protected String getTestHeader() {
    return "Testing: " + name.getMethodName();
  }

  @BeforeEach
  public final void initializeMuleTest(TestInfo testInfo) {
    skipTestWhenDisabledInCurrentEnvironment(testInfo.getTestMethod().map(Method::getName).orElse(""));
    printTestHeader(testInfo);
  }

  private void printTestHeader(TestInfo testInfo) {
    if (verbose) {
      System.out.println(" => " + getTestHeader(testInfo));
    }
  }

  protected String getTestHeader(TestInfo testInfo) {
    return "Testing: " + testInfo.getDisplayName();
  }

  private void skipTestWhenDisabledInCurrentEnvironment(String testMethodName) {
    assumeThat(this, new BaseMatcher<AbstractMuleTestCase>() {

      @Override
      public boolean matches(Object o) {
        return !(isDisabledInThisEnvironment() || isDisabledInThisEnvironment(testMethodName));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Test " + testMethodName + " disabled in this environment");
      }
    });
  }

  /**
   * Indicates whether the test should fail when a timeout is reached.
   * <p/>
   * This feature was added to support old test cases that depend on 3rd-party resources such as a public web service. In such
   * cases it may be desirable to not fail the test upon timeout but rather to simply log a warning.
   *
   * @return true if it must fail on timeout and false otherwise. Default value is true.
   */
  protected boolean isFailOnTimeout() {
    return true;
  }

  /**
   * @return A dummy instance of {@link ArtifactCoordinates} for the purpose of running tests
   * @since 4.5.0
   */
  protected ArtifactCoordinates getTestArtifactCoordinates() {
    return new ArtifactCoordinates() {

      @Override
      public String getGroupId() {
        return "org.mule.tests";
      }

      @Override
      public String getArtifactId() {
        return "mule-test";
      }

      @Override
      public String getVersion() {
        return "1.0.0";
      }
    };
  }

  @After
  public void clearRequestContext() {
    setCurrentEvent(null);
  }

  protected static List<String> collectThreadNames() {
    List<String> threadNames = new ArrayList<>();
    for (Thread t : getAllStackTraces().keySet()) {
      if (t.isAlive()) {
        threadNames.add(t.getName() + " - " + t.getId());
      }
    }
    sort(threadNames);
    return threadNames;

  }


  private static String testCaseName;

  @BeforeClass
  public static void clearTestCaseName() {
    testCaseName = null;
  }

  @Before
  public void takeTestCaseName() {
    if (testCaseName == null) {
      testCaseName = this.getClass().getName();
    }
  }

  private static final List<Flow> testFlows = new ArrayList<>();

  protected static Flow getTestFlow(MuleContext context) throws MuleException {
    Flow flow = MuleTestUtils.getTestFlow(context);
    testFlows.add(flow);
    return flow;
  }

  @AfterClass
  public static void clearTestFlows() {
    if (!testFlows.isEmpty()) {
      disposeIfNeeded(testFlows, LOGGER);
      testFlows.clear();
    }
  }

  private CoreEvent _testEvent;
  private CoreEvent _nullPayloadEvent;

  /**
   * Creates and caches a test {@link CoreEvent} instance for the scope of the current test method.
   *
   * @return test event.
   * @throws MuleException
   */
  protected CoreEvent testEvent() throws MuleException {
    if (_testEvent == null) {
      _testEvent = newEvent();
    }
    return _testEvent;
  }

  /**
   * Create a new {@link CoreEvent} for each invocation. Useful if multiple distinct event instances are needed in a single test
   * method.
   *
   * @return new test event.
   * @throws MuleException
   */
  protected <E extends CoreEvent> E newEvent() throws MuleException {
    return (E) getEventBuilder().message(of(TEST_PAYLOAD)).build();
  }

  /**
   * Allows subclasses to provide a builder configured in a different way
   *
   * @return a event builder to use to build a test event
   * @throws MuleException
   */
  protected <B extends CoreEvent.Builder> B getEventBuilder() throws MuleException {
    return (B) eventBuilder();
  }

  protected CoreEvent nullPayloadEvent() throws MuleException {
    if (_nullPayloadEvent == null) {
      _nullPayloadEvent = eventBuilder().message(of(null)).build();
    }
    return _nullPayloadEvent;
  }

  @After
  public void clearTestEvents() throws MuleException {
    _testEvent = null;
    _nullPayloadEvent = null;
  }

  @AfterClass
  public static void dumpFilteredThreadsInTest() {
    List<String> currentThreads = collectThreadNames();
    int filteredThreads = 0;
    StringBuilder builder = new StringBuilder();
    for (String threadName : currentThreads) {
      if (!nameIn(threadName, "[MuleRuntime]", "Finalizer", "Monitor Ctrl-Break", "Reference Handler", "Signal Dispatcher",
                  "main")) {
        builder.append("\n-> ").append(threadName);
        filteredThreads++;
      }
    }
    if (filteredThreads > 0) {
      logThreadsResult(format("Hung threads count: %d. Test case: %s. Thread names:%s", filteredThreads, testCaseName,
                              builder.toString()));
    } else {
      logThreadsResult(format("No hung threads. Test case: %s", testCaseName));
    }
  }

  private static boolean nameIn(String threadName, String... values) {
    String threadNameLowercase = threadName.toLowerCase();
    if (values != null) {
      for (String value : values) {
        if (threadNameLowercase.startsWith(value.toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Utility method to add a mock component location.
   *
   * @param component object to add the location.
   */
  protected void addMockComponentLocation(Component component) {
    ComponentLocation componentLocation = mock(ComponentLocation.class, RETURNS_DEEP_STUBS);
    when(componentLocation.getLineInFile()).thenReturn(empty());
    when(componentLocation.getLine()).thenReturn(OptionalInt.empty());
    when(componentLocation.getFileName()).thenReturn(empty());
    component.setAnnotations(singletonMap(LOCATION_KEY, componentLocation));
  }

  private static final transient String THREAD_RESULT_LINE = StringUtils.repeat('-', 80);

  private static void logThreadsResult(String result) {
    LOGGER.warn(format("\n%s\n%s\n%s\n", THREAD_RESULT_LINE, result, THREAD_RESULT_LINE));
  }

  /**
   * {@link FeatureFlaggingService} used for testing with all the features enabled
   *
   * @return
   */
  protected FeatureFlaggingService getFeatureFlaggingService() {
    return feature -> true;
  }

}
