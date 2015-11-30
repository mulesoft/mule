/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.junit.Assume.assumeThat;
import org.mule.RequestContext;
import org.mule.tck.junit4.rule.WarningTimeout;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule test cases. This
 * implementation provides services to test code for creating mock and test
 * objects.
 */
public abstract class AbstractMuleTestCase
{
    public static final String TESTING_MODE_PROPERTY_NAME = "mule.testingMode";

    public static final int DEFAULT_TEST_TIMEOUT_SECS = 60;

    public static final String TEST_TIMEOUT_SYSTEM_PROPERTY = "mule.test.timeoutSecs";

    /**
     * Indicates whether the text boxes will be logged when starting each test case.
     */
    private static final boolean verbose;

    /**
     * Indicates if the current test class was excluded using the mule test
     * exclusion files. Test are executed sequentially, so is not required to
     * maintain a list of classes.
     */
    private static Boolean excluded = null;

    static
    {
        String muleOpts = SystemUtils.getenv("MULE_TEST_OPTS");
        if (StringUtils.isNotBlank(muleOpts))
        {
            Map<String, String> parsedOpts = SystemUtils.parsePropertyDefinitions(muleOpts);
            String optVerbose = parsedOpts.get("mule.verbose");
            verbose = Boolean.valueOf(optVerbose);
        }
        else
        {
            verbose = true;
        }

        System.setProperty(TESTING_MODE_PROPERTY_NAME, StringUtils.EMPTY);

        // register the custom UrlStreamHandlerFactory.
        MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    }

    protected final transient Log logger = LogFactory.getLog(this.getClass());

    /**
     * Should be set to a string message describing any prerequisites not met.
     */
    private boolean offline = "true".equalsIgnoreCase(System.getProperty("org.mule.offline"));

    private int testTimeoutSecs = getTimeoutSystemProperty();

    @Rule
    public TestName name = new TestName();

    @Rule
    public TestRule globalTimeout = createTestTimeoutRule();

    public AbstractMuleTestCase()
    {
        if (excluded == null)
        {
            excluded = isTestIncludedInExclusionFile(this);
        }
    }

    /**
     * Creates the timeout rule that will be used to run the test.
     *
     * @return the rule used to check for test execution timeouts.
     */
    protected TestRule createTestTimeoutRule()
    {
        int millisecondsTimeout = getTestTimeoutSecs() * 1000;

        if (isFailOnTimeout())
        {
            return new Timeout(millisecondsTimeout);
        }
        else
        {
            return new WarningTimeout(millisecondsTimeout);
        }
    }

    /**
     * Reads the mule-exclusion file for the current test class and
     *
     * @param test
     */
    protected boolean isTestIncludedInExclusionFile(AbstractMuleTestCase test)
    {
        boolean result = false;

        final String testName = test.getClass().getName();
        try
        {
            // We find the physical classpath root URL of the test class and
            // use that to find the correct resource. Works fine everywhere,
            // regardless of classloaders. See MULE-2414
            URL classUrl = ClassUtils.getClassPathRoot(test.getClass());
            URLClassLoader tempClassLoader = new URLClassLoader(new URL[] {classUrl});
            URL fileUrl = tempClassLoader.getResource("mule-test-exclusions.txt");
            if (fileUrl != null)
            {
                InputStream in = null;
                try
                {
                    in = fileUrl.openStream();

                    // this iterates over all lines in the exclusion file
                    Iterator<?> lines = IOUtils.lineIterator(in, "UTF-8");

                    // ..and this finds non-comments that match the test case name
                    result = IteratorUtils.filteredIterator(lines, new Predicate()
                    {
                        @Override
                        public boolean evaluate(Object object)
                        {
                            return StringUtils.equals(testName, StringUtils.trimToEmpty((String) object));
                        }
                    }).hasNext();
                }
                finally
                {
                    IOUtils.closeQuietly(in);
                }
            }
        }
        catch (IOException ioex)
        {
            // ignore
        }

        return result;
    }

    /**
     * Defines the number of seconds that a test has in order to run before
     * throwing a timeout. If the property if not defined then uses the
     * <code>DEFAULT_MULE_TEST_TIMEOUT_SECS</code> constant.
     *
     * @return the timeout value expressed in seconds
     */
    protected int getTimeoutSystemProperty()
    {
        String timeoutString = System.getProperty(TEST_TIMEOUT_SYSTEM_PROPERTY, null);
        if (timeoutString == null)
        {
            // unix style: MULE_TEST_TIMEOUTSECS
            String variableName = TEST_TIMEOUT_SYSTEM_PROPERTY.toUpperCase().replace(".", "_");
            timeoutString = System.getenv(variableName);
        }

        int result = DEFAULT_TEST_TIMEOUT_SECS;
        if (timeoutString != null)
        {
            try
            {
                result = Integer.parseInt(timeoutString);
            }
            catch (NumberFormatException e)
            {
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
    protected boolean isDisabledInThisEnvironment()
    {
        return false;
    }

    /**
     * Indicates whether this test has been explicitly disabled through the configuration
     * file loaded by TestInfo.
     *
     * @return whether the test has been explicitly disabled
     */
    protected boolean isExcluded()
    {
        return excluded;
    }

    /**
     * Should this test run?
     *
     * @param testMethodName name of the test method
     * @return whether the test should execute in the current environment
     */
    protected boolean isDisabledInThisEnvironment(String testMethodName)
    {
        return false;
    }

    public boolean isOffline(String method)
    {
        if (offline)
        {
            logger.warn(StringMessageUtils.getBoilerPlate(
                    "Working offline cannot run test: " + method, '=', 80));
        }

        return offline;
    }

    /**
     * Defines the timeout in seconds that will be used to run the test.
     *
     * @return the timeout in seconds
     */
    public int getTestTimeoutSecs()
    {
        return testTimeoutSecs;
    }

    @Before
    public final void initializeMuleTest()
    {
        skipTestWhenExcluded();
        skipTestWhenDisabledInCurrentEnvironment();
        printTestHeader();
    }

    private void printTestHeader()
    {
        if (verbose)
        {
            System.out.println(StringMessageUtils.getBoilerPlate(getTestHeader(), '=', 80));
        }
    }

    protected String getTestHeader()
    {
        return "Testing: " + name.getMethodName();
    }

    private void skipTestWhenExcluded()
    {
        assumeThat(this, new BaseMatcher<AbstractMuleTestCase>()
        {
            @Override
            public boolean matches(Object o)
            {
                return !isExcluded();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("Test " + name.getMethodName() + " is excluded");
            }
        });
    }

    private void skipTestWhenDisabledInCurrentEnvironment()
    {
        assumeThat(this, new BaseMatcher<AbstractMuleTestCase>()
        {
            @Override
            public boolean matches(Object o)
            {
                return !(isDisabledInThisEnvironment() || isDisabledInThisEnvironment(name.getMethodName()));
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("Test " + name.getMethodName() + " disabled in this environment");
            }
        });
    }

    /**
     * Indicates whether the test should fail when a timeout is reached.
     * <p/>
     * This feature was added to support old test cases that depend on 3rd-party
     * resources such as a public web service. In such cases it may be desirable
     * to not fail the test upon timeout but rather to simply log a warning.
     *
     * @return true if it must fail on timeout and false otherwise. Default value
     *         is true.
     */
    protected boolean isFailOnTimeout()
    {
        return true;
    }

    @After
    public final void clearRequestContext()
    {
        RequestContext.clear();
    }

    @AfterClass
    public static final void clearExcludedFlag()
    {
        excluded = null;
    }

    private static List<String> collectThreadNames()
    {
        List<String> threadNames = new ArrayList<String>();
        for (Thread t : Thread.getAllStackTraces().keySet())
        {
            if (t.isAlive())
            {
                threadNames.add(t.getName() + " - " + t.getId());
            }
        }
        Collections.sort(threadNames);
        return threadNames;
    }


    private static String testCaseName;

    @BeforeClass
    public static void clearTestCaseName()
    {
        testCaseName = null;
    }

    @Before
    public void takeTestCaseName()
    {
        if (testCaseName == null)
        {
            testCaseName = this.getClass().getName();
        }
    }

    @AfterClass
    public static void dumpFilteredThreadsInTest()
    {
        List<String> currentThreads = collectThreadNames();
        int filteredThreads = 0;
        StringBuilder builder = new StringBuilder();
        for (String threadName : currentThreads)
        {
            if (!nameIn(threadName, "Finalizer", "Monitor Ctrl-Break", "Reference Handler", "Signal Dispatcher", "main"))
            {
                builder.append("\n-> ").append(threadName);
                filteredThreads++;
            }
        }
        if (filteredThreads > 0)
        {
            logThreadsResult(String.format("Hung threads count: %d. Test case: %s. Thread names:%s", filteredThreads, testCaseName, builder.toString()));
        }
        else
        {
            logThreadsResult(String.format("No hung threads. Test case: %s", testCaseName));
        }
    }

    private static boolean nameIn(String threadName, String... values)
    {
        String threadNameLowercase = threadName.toLowerCase();
        if (values != null)
        {
            for (String value : values)
            {
                if (threadNameLowercase.startsWith(value.toLowerCase()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static final transient String THREAD_RESULT_LINE = StringUtils.repeat('-', 80);
    private static final transient Log LOGGER = LogFactory.getLog(AbstractMuleTestCase.class);

    private static void logThreadsResult(String result)
    {
        LOGGER.warn(String.format("\n%s\n%s\n%s\n", THREAD_RESULT_LINE, result, THREAD_RESULT_LINE));
    }

}
