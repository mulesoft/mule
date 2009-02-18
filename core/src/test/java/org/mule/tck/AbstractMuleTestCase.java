/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleSession;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.util.FileUtils;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule testcases. This
 * implementation provides services to test code for creating mock and test objects.
 */
public abstract class AbstractMuleTestCase extends TestCase implements TestCaseWatchdogTimeoutHandler
{

    /**
     * Top-level directories under <code>.mule</code> which are not deleted on each
     * test case recycle. This is required, e.g. to play nice with transaction manager
     * recovery service object store.
     */
    public static final String[] IGNORED_DOT_MULE_DIRS = new String[]{"transaction-log"};

    /**
     * Name of a property to override the default test watchdog timeout.
     * @see #DEFAULT_MULE_TEST_TIMEOUT_SECS 
     */
    public static final String PROPERTY_MULE_TEST_TIMEOUT = "mule.test.timeoutSecs";
    
    /**
     * Default test watchdog timeout in seconds.
     */
    public static final int DEFAULT_MULE_TEST_TIMEOUT_SECS = 60;

    protected static MuleContext muleContext;

    /**
     * This flag controls whether the text boxes will be logged when starting each test case.
     */
    private static final boolean verbose;

    // A Map of test case extension objects. JUnit creates a new TestCase instance for
    // every method, so we need to record metainfo outside the test.
    private static final Map testInfos = Collections.synchronizedMap(new HashMap());

    // A logger that should be suitable for most test cases.
    protected final transient Log logger = LogFactory.getLog(this.getClass());

    /**
     * Start the muleContext once it's configured (defaults to false for AbstractMuleTestCase, true for FunctionalTestCase).
     */
    private boolean startContext = false;

    // Should be set to a string message describing any prerequisites not met.
    private boolean offline = "true".equalsIgnoreCase(System.getProperty("org.mule.offline"));

    // Barks if the test exceeds its time limit
    private TestCaseWatchdog watchdog;

    static
    {
        String muleOpts = SystemUtils.getenv("MULE_TEST_OPTS");
        if (StringUtils.isNotBlank(muleOpts))
        {
            Map parsedOpts = SystemUtils.parsePropertyDefinitions(muleOpts);
            String optVerbose = (String) parsedOpts.get("mule.verbose");
            verbose = Boolean.valueOf(optVerbose).booleanValue();
        }
        else
        {
            // per default, revert to the old behaviour
            verbose = true;
        }

        // register the custom UrlStreamHandlerFactory.
        MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    }

    /**
     * Convenient test message for unit testing.
     */
    public static final String TEST_MESSAGE = "Test Message";

    /**
     * Default timeout for multithreaded tests (using CountDownLatch, WaitableBoolean, etc.),
     * in milliseconds.  The higher this value, the more reliable the test will be, so it
     * should be set high for Continuous Integration.  However, this can waste time during
     * day-to-day development cycles, so you may want to temporarily lower it while debugging.
     */
    public static final long LOCK_TIMEOUT = 30000;

    /**
     * Default timeout for waiting for responses
     */
    public static final int RECEIVE_TIMEOUT = 5000;

    /**
     * Use this as a semaphore to the unit test to indicate when a callback has successfully been called.
     */
    protected Latch callbackCalled;

    /**
     * When a test case depends on a 3rd-party resource such as a public web service,
     * it may be desirable to not fail the test upon timeout but rather to simply log 
     * a warning.
     */
    private boolean failOnTimeout = true;

    private int timeoutSecs = DEFAULT_MULE_TEST_TIMEOUT_SECS;

    public AbstractMuleTestCase()
    {
        super();

        TestInfo info = (TestInfo) testInfos.get(getClass().getName());
        if (info == null)
        {
            info = this.createTestInfo();
            testInfos.put(getClass().getName(), info);
        }
        this.registerTestMethod();
    }

    protected void registerTestMethod()
    {
        if (this.getName() != null)
        {
            this.getTestInfo().incTestCount(getName());
        }
    }

    public void setName(String name)
    {
        super.setName(name);
        registerTestMethod();
    }

    protected TestInfo createTestInfo()
    {
        return new TestInfo(this);
    }

    protected TestInfo getTestInfo()
    {
        return (TestInfo) testInfos.get(this.getClass().getName());
    }

    private void clearInfo()
    {
        testInfos.remove(this.getClass().getName());
    }

    public String getName()
    {
        if (verbose && super.getName() != null)
        {
            return super.getName().substring(4).replaceAll("([A-Z])", " $1").toLowerCase() + " ";
        }
        return super.getName();
    }

    public void run(TestResult result)
    {
        if (this.isExcluded())
        {
            if (verbose)
            {
                logger.info(this.getClass().getName() + " excluded");
            }
            return;
        }

        if (this.isDisabledInThisEnvironment())
        {
            if (verbose)
            {
                logger.info(this.getClass().getName() + " disabled");
            }
            return;
        }

        super.run(result);
    }

    /**
     * Shamelessly copy from Spring's ConditionalTestCase so in MULE-2.0 we can extend
     * this class from ConditionalTestCase.
     * <p/>
     * Subclasses can override <code>isDisabledInThisEnvironment</code> to skip a single test.
     */
    public void runBare() throws Throwable
    {
        // getName will return the name of the method being run. Use the real JUnit implementation,
        // this class has a different implementation
        if (this.isDisabledInThisEnvironment(super.getName()))
        {
            logger.warn(this.getClass().getName() + "." + super.getName() + " disabled in this environment");
            return;
        }

        // Let JUnit handle execution
        super.runBare();
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
        return getTestInfo().isExcluded();
    }

    /**
     * Should this test run?
     *
     * @param testMethodName name of the test method
     * @return whether the test should execute in the current envionment
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

    protected boolean isDisposeManagerPerSuite()
    {
        return getTestInfo().isDisposeManagerPerSuite();
    }

    protected void setDisposeManagerPerSuite(boolean val)
    {
        getTestInfo().setDisposeManagerPerSuite(val);
    }

    public int getTimeoutSecs()
    {
        return timeoutSecs;
    }

    protected TestCaseWatchdog createWatchdog()
    {

        try
        {
            timeoutSecs = Integer.parseInt(System.getProperty(PROPERTY_MULE_TEST_TIMEOUT, "" + DEFAULT_MULE_TEST_TIMEOUT_SECS));
        }
        catch (NumberFormatException e)
        {
            // if something went wrong
            timeoutSecs = DEFAULT_MULE_TEST_TIMEOUT_SECS;
        }
        return new TestCaseWatchdog(timeoutSecs, TimeUnit.SECONDS, this);
    }

    public void handleTimeout(long timeout, TimeUnit unit)
    {
        String msg = "Timeout of " + unit.toMillis(timeout) + "ms exceeded - exiting VM!";
        
        if (failOnTimeout)
        {
            logger.fatal(msg);
            Runtime.getRuntime().halt(1);
        }
        else
        {
            logger.warn(msg);
        }
    }

    /**
     * Normal JUnit method
     * @throws Exception
     *
     * @see #doSetUp()
     */
    protected final void setUp() throws Exception
    {
        // start a watchdog thread
        watchdog = createWatchdog();
        watchdog.start();

        if (verbose)
        {
            System.out.println(StringMessageUtils.getBoilerPlate("Testing: " + toString(), '=', 80));
        }

        try
        {
            if (getTestInfo().getRunCount() == 0)
            {
                if (getTestInfo().isDisposeManagerPerSuite())
                {
                    // We dispose here jut in case
                    disposeManager();
                }
                suitePreSetUp();
            }
            if (!getTestInfo().isDisposeManagerPerSuite())
            {
                // We dispose here just in case
                disposeManager();
            }

            muleContext = createMuleContext();
            if (isStartContext() && null != muleContext && muleContext.isStarted() == false)
            {
                muleContext.start();
            }

            doSetUp();
        }
        catch (Exception e)
        {
            getTestInfo().incRunCount();
            throw e;
        }
    }

    protected MuleContext createMuleContext() throws Exception
    {
        // Should we set up the manager for every method?
        MuleContext context;
        if (getTestInfo().isDisposeManagerPerSuite() && muleContext != null)
        {
            context = muleContext;
        }
        else
        {
            MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
            List builders = new ArrayList();
            builders.add(new SimpleConfigurationBuilder(getStartUpProperties()));
            builders.add(getBuilder());
            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            configureMuleContext(contextBuilder);
            context = muleContextFactory.createMuleContext(builders, contextBuilder);
        }
        return context;
    }

    /**
     * Override this method to set properties of the MuleContextBuilder before it is
     * used to create the MuleContext.
     */
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        contextBuilder.setWorkListener(new TestingWorkListener());
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new DefaultsConfigurationBuilder();
    }

    protected String getConfigurationResources()
    {
        return StringUtils.EMPTY;
    }

    protected Properties getStartUpProperties()
    {
        return null;
    }

    /**
     * Run <strong>before</strong> any testcase setup.
     * This is called once only before the test suite runs.
     */
    protected void suitePreSetUp() throws Exception
    {
        // nothing to do
    }

    /**
     * Run <strong>after</strong> all testcase teardowns.
     * This is called once only after all the tests in the suite have run.
     */
    protected void suitePostTearDown() throws Exception
    {
        // nothing to do
    }

    /**
     * Normal JUnit method
     * @throws Exception
     *
     * @see #doTearDown()
     */
    protected final void tearDown() throws Exception
    {
        try
        {
            doTearDown();

            if (!getTestInfo().isDisposeManagerPerSuite())
            {
                disposeManager();
            }
        }
        finally
        {
            try
            {
                getTestInfo().incRunCount();
                if (getTestInfo().getRunCount() == getTestInfo().getTestCount())
                {
                    try
                    {
                        suitePostTearDown();
                    }
                    finally
                    {
                        clearInfo();
                        disposeManager();
                    }
                }
            }
            finally
            {
                // remove the watchdog thread in any case
                watchdog.cancel();
            }
        }
    }

    protected void disposeManager()
    {
        try
        {
            if (muleContext != null && !(muleContext.isDisposed() || muleContext.isDisposing()))
            {
                muleContext.dispose();

                final String workingDir = muleContext.getConfiguration().getWorkingDirectory();
                // do not delete TM recovery object store, everything else is good to
                // go
                FileUtils.deleteTree(FileUtils.newFile(workingDir), IGNORED_DOT_MULE_DIRS);
            }
            FileUtils.deleteTree(FileUtils.newFile("./ActiveMQ"));
        }
        finally
        {
            muleContext = null;
            MuleServer.setMuleContext(null);
        }
    }

    /**
     * Exactly the same a {@link #setUp()} in normal JUnit test cases.  this is called <strong>before</strong> a test
     * method has been called.
     *
     * @throws Exception if something fails that should halt the testcase
     */
    protected void doSetUp() throws Exception
    {
        // template method
    }

    /**
     * Exactly the same a {@link #tearDown()} in normal JUnit test cases.  this is called <strong>after</strong> a test
     * method has been called.
     *
     * @throws Exception if something fails that should halt the testcase
     */
    protected void doTearDown() throws Exception
    {
        // template method
    }

    public static InboundEndpoint getTestInboundEndpoint(String name) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, String uri) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, null, null, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, null, null, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, List transformers) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, null, transformers, null, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, List transformers) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, null, transformers, null, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, String uri, List transformers, Filter filter, Map properties) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, transformers, filter, properties);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri, List transformers, Filter filter, Map properties) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties);
    }

    public static MuleEvent getTestEvent(Object data, Service service) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, service, muleContext);
    }

    public static MuleEvent getTestEvent(Object data) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, muleContext);
    }

    public static MuleEvent getTestInboundEvent(Object data) throws Exception
    {
        return MuleTestUtils.getTestInboundEvent(data, muleContext);
    }

    public static MuleEventContext getTestEventContext(Object data) throws Exception
    {
        return MuleTestUtils.getTestEventContext(data, muleContext);
    }

    public static Transformer getTestTransformer() throws Exception
    {
        return MuleTestUtils.getTestTransformer();
    }

    public static MuleEvent getTestEvent(Object data, ImmutableEndpoint endpoint) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, endpoint, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, Service service, ImmutableEndpoint endpoint)
            throws Exception
    {
        return MuleTestUtils.getTestEvent(data, service, endpoint, muleContext);
    }

    public static MuleSession getTestSession(Service service, MuleContext context)
    {
        return MuleTestUtils.getTestSession(service, context);
    }

    public static TestConnector getTestConnector() throws Exception
    {
        return MuleTestUtils.getTestConnector(muleContext);
    }

    public static Service getTestService() throws Exception
    {
        return MuleTestUtils.getTestService(muleContext);
    }

    public static Service getTestService(String name, Class clazz) throws Exception
    {
        return MuleTestUtils.getTestService(name, clazz, muleContext);
    }

    public static Service getTestService(String name, Class clazz, Map props) throws Exception
    {
        return MuleTestUtils.getTestService(name, clazz, props, muleContext);
    }

    public static class TestInfo
    {
        /**
         * Whether to dispose the manager after every method or once all tests for
         * the class have run
         */
        private final String name;
        private boolean disposeManagerPerSuite = false;
        private boolean excluded = false;
        private volatile int testCount = 0;
        private volatile int runCount = 0;
        // @GuardedBy(this)
        private Set registeredTestMethod = new HashSet();

        // this is a shorter version of the snippet from:
        // http://www.davidflanagan.com/blog/2005_06.html#000060
        // (see comments; DF's "manual" version works fine too)
        public static URL getClassPathRoot(Class clazz)
        {
            CodeSource cs = clazz.getProtectionDomain().getCodeSource();
            return (cs != null ? cs.getLocation() : null);
        }

        public TestInfo(TestCase test)
        {
            this.name = test.getClass().getName();

            // load test exclusions
            try
            {
                // We find the physical classpath root URL of the test class and
                // use that to find the correct resource. Works fine everywhere,
                // regardless of classloaders. See MULE-2414
                URL[] urls = new URL[]{getClassPathRoot(test.getClass())};
                URL fileUrl = new URLClassLoader(urls).getResource("mule-test-exclusions.txt");

                if (fileUrl != null)
                {
                    // in case .txt is in jar
                    URI fileUri = new URI(StringUtils.removeStart(fileUrl.toString(), "jar:"));

                    // this iterates over all lines in the exclusion file
                    Iterator lines = FileUtils.lineIterator(FileUtils.newFile(fileUri));

                    // ..and this finds non-comments that match the test case name
                    excluded = IteratorUtils.filteredIterator(lines, new Predicate()
                    {
                        public boolean evaluate(Object object)
                        {
                            return StringUtils.equals(name, StringUtils.trimToEmpty((String) object));
                        }
                    }).hasNext();
                }
            }
            catch (IOException ioex)
            {
                // ignore
            }
            catch (URISyntaxException e)
            {
                // ignore
            }
        }

        public int getTestCount()
        {
            return testCount;
        }

        public synchronized void incTestCount(String name)
        {
            if (!registeredTestMethod.contains(name))
            {
                testCount++;
                registeredTestMethod.add(name);
            }
        }

        public int getRunCount()
        {
            return runCount;
        }

        public void incRunCount()
        {
            runCount++;
        }

        public String getName()
        {
            return name;
        }

        public boolean isDisposeManagerPerSuite()
        {
            return disposeManagerPerSuite;
        }

        public void setDisposeManagerPerSuite(boolean disposeManagerPerSuite)
        {
            this.disposeManagerPerSuite = disposeManagerPerSuite;
        }

        public boolean isExcluded()
        {
            return excluded;
        }

        public synchronized String toString()
        {
            StringBuffer buf = new StringBuffer();
            return buf.append(name).append(", (").append(runCount).append(" / ").append(testCount).append(
                    ") tests run, disposePerSuite=").append(disposeManagerPerSuite).toString();
        }
    }

    protected boolean isStartContext()
    {
        return startContext;
    }

    protected void setStartContext(boolean startContext)
    {
        this.startContext = startContext;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }
}
