/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleSession;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule testcases. This
 * implementation provides services to test code for creating mock and test objects.
 * @deprecated Use {@link org.mule.tck.junit4.AbstractMuleTestCase}
 */
@Deprecated
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
     *
     * @see #DEFAULT_MULE_TEST_TIMEOUT_SECS
     */
    public static final String PROPERTY_MULE_TEST_TIMEOUT = "mule.test.timeoutSecs";

    /**
     * Default test watchdog timeout in seconds.
     */
    public static final int DEFAULT_MULE_TEST_TIMEOUT_SECS = 60;

    /**
     * If the annotations module is on the classpath, also enable annotations config builder
     */
    public static final String CLASSNAME_ANNOTATIONS_CONFIG_BUILDER = "org.mule.config.AnnotationsConfigurationBuilder";


    protected static MuleContext muleContext;

    /**
     * This flag controls whether the text boxes will be logged when starting each test case.
     */
    private static final boolean verbose;

    // A Map of test case extension objects. JUnit creates a new TestCase instance for
    // every method, so we need to record metainfo outside the test.
    private static final Map<String, TestInfo> testInfos = Collections.synchronizedMap(new HashMap<String, TestInfo>());

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

    protected int numPorts = 0;

    public List<Integer> ports = null;

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
     * Timeout used for the test watchdog
     */
    protected int testTimeoutSecs = DEFAULT_MULE_TEST_TIMEOUT_SECS;

    /**
     * When a test case depends on a 3rd-party resource such as a public web service,
     * it may be desirable to not fail the test upon timeout but rather to simply log
     * a warning.
     */
    private boolean failOnTimeout = true;

    /**
     * This is just a reference to the main thread running the current thread. It is
     * set in the {@link #setUp()} method.
     */
    private volatile Thread currentTestRunningThread;

    public AbstractMuleTestCase()
    {
        super();

        TestInfo info = getTestInfo();
        if (info == null)
        {
            info = this.createTestInfo();
            testInfos.put(getClass().getName(), info);
        }
        registerTestMethod();
        initTestTimeoutSecs();
    }

    protected void registerTestMethod()
    {
        if (this.getName() != null)
        {
            this.getTestInfo().incTestCount(getName());
        }
    }

    protected void initTestTimeoutSecs()
    {
        String timeoutString = System.getProperty(PROPERTY_MULE_TEST_TIMEOUT, null);
        if (timeoutString == null)
        {
            // unix style: MULE_TEST_TIMEOUTSECS
            String variableName = PROPERTY_MULE_TEST_TIMEOUT.toUpperCase().replace(".", "_");
            timeoutString = System.getenv(variableName);
        }

        if (timeoutString != null)
        {
            try
            {
                testTimeoutSecs = Integer.parseInt(timeoutString);
            }
            catch (NumberFormatException nfe)
            {
                // the default still applies
            }
        }
    }

    @Override
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
        return testInfos.get(this.getClass().getName());
    }

    private void clearInfo()
    {
        testInfos.remove(this.getClass().getName());
    }

    @Override
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
    @Override
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

    public int getTestTimeoutSecs()
    {
        return testTimeoutSecs;
    }

    protected TestCaseWatchdog createWatchdog()
    {
        return new TestCaseWatchdog(testTimeoutSecs, TimeUnit.SECONDS, this);
    }

    public void handleTimeout(long timeout, TimeUnit unit)
    {
        String msg = "Timeout of " + unit.toMillis(timeout) + "ms exceeded (modify via -Dmule.test.timeoutSecs=XX)";

        if (failOnTimeout)
        {
            logger.fatal(msg + " - Attempting to interrupt thread for test " + this.getName());
            if (currentTestRunningThread != null)
            {
                currentTestRunningThread.interrupt();
            }
            giveTheTestSomeTimeToCleanUpAndThenKillIt("Interrupting didn't work. Killing the VM!. Test "
                                                      + this.getName() + " did not finish correctly.");
        }
        else
        {
            logger.warn(msg);
        }
    }

    protected void giveTheTestSomeTimeToCleanUpAndThenKillIt(String messageIfNeedToKill)
    {
        try
        {
            Thread.sleep(5000);
            logger.fatal(messageIfNeedToKill);
            Runtime.getRuntime().halt(1);
        }
        catch (InterruptedException e)
        {
            logger.info(
                "Test thread has been interrupted, probably bt the call to watchdog.cancel() in teardown method.",
                e);
        }
    }

    /**
     * Normal JUnit method
     *
     * @throws Exception
     * @see #doSetUp()
     */
    @Override
    protected final void setUp() throws Exception
    {
        // start a watchdog thread
        watchdog = createWatchdog();
        watchdog.start();

        // set up the free ports
        if (numPorts > 0)
        {
            //find some free ports
            ports = PortUtils.findFreePorts(numPorts);

            //set the port properties
            setPortProperties();
        }

        currentTestRunningThread = Thread.currentThread();

        printTestHeader();

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

            // wait for Mule to fully start when requested (default)

            // latch ref needs to be final for listener use, wrap in atomic ref
            final AtomicReference<Latch> contextStartedLatch = new AtomicReference<Latch>();
            if (isStartContext() && null != muleContext && muleContext.isStarted() == false)
            {
                contextStartedLatch.set(new Latch());
                muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
                {
                    public void onNotification(MuleContextNotification notification)
                    {
                        if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                        {
                            contextStartedLatch.get().countDown();
                        }
                    }
                });
                muleContext.start();
            }

            // if it's null, than
            if (contextStartedLatch.get() != null)
            {
                // wait no more than 20 secs
                contextStartedLatch.get().await(20, TimeUnit.SECONDS);
            }
            doSetUp();
        }
        catch (Exception e)
        {
            getTestInfo().incRunCount();
            throw e;
        }
    }

    protected void printTestHeader()
    {
        if (verbose)
        {
            System.out.println(StringMessageUtils.getBoilerPlate(getTestHeader(), '=', 80));
        }
    }

    protected String getTestHeader()
    {
        return "Testing: " + getName();
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
            List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
            builders.add(new SimpleConfigurationBuilder(getStartUpProperties()));
            //If the annotations module is on the classpath, add the annotations config builder to the list
            //This will enable annotations config for this instance
            if (ClassUtils.isClassOnPath(CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
            {
                builders.add((ConfigurationBuilder) ClassUtils.instanciateClass(CLASSNAME_ANNOTATIONS_CONFIG_BUILDER,
                        ClassUtils.NO_ARGS, getClass()));
            }
            builders.add(getBuilder());
            addBuilders(builders);
            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            configureMuleContext(contextBuilder);
            context = muleContextFactory.createMuleContext(builders, contextBuilder);
            if (!isGracefulShutdown())
            {
                ((DefaultMuleConfiguration) context.getConfiguration()).setShutdownTimeout(0);
            }
        }
        return context;
    }

    //This sohuldn't be needed by Test cases but can be used by base testcases that wish to add further builders when
    //creating the MuleContext.

    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        //No op
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
     *
     * @throws Exception
     * @see #doTearDown()
     */
    @Override
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
        RequestContext.clear();
    }

    public static InboundEndpoint getTestInboundEndpoint(String name) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext);
    }

    public static InboundEndpoint getTestInboundEndpoint(MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(mep, muleContext);
    }

    public static InboundEndpoint getTestTransactedInboundEndpoint(MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestTransactedInboundEndpoint(mep, muleContext);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, String uri) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, null, null, null, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, null, null, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, List<Transformer> transformers) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, null, transformers, null, null, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, List<Transformer> transformers) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, null, transformers, null, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, String uri,
        List<Transformer> transformers, Filter filter, Map<Object, Object> properties, Connector connector) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, transformers, filter, properties, connector);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri,
        List<Transformer> transformers, Filter filter, Map<Object, Object> properties) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name,
                                                          String uri,
                                                          List<Transformer> transformers,
                                                          Filter filter,
                                                          Map<Object, Object> properties) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, transformers, filter, properties);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri,
        List<Transformer> transformers, Filter filter, Map<Object, Object> properties, Connector connector) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties, connector);
    }

    public static MuleEvent getTestEvent(Object data, Service service) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, service, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, Service service, MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, service, mep, muleContext);
    }

    public static MuleEvent getTestEvent(Object data) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEvent getTestEventUsingFlow(Object data) throws Exception
    {
        return MuleTestUtils.getTestEventUsingFlow(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, mep, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, MuleSession session) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, session, muleContext);
    }

//    public static MuleEvent getTestInboundEvent(Object data) throws Exception
//    {
//        return MuleTestUtils.getTestInboundEvent(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
//    }
//
//    public static MuleEvent getTestInboundEvent(Object data, MessageExchangePattern mep) throws Exception
//    {
//        return MuleTestUtils.getTestInboundEvent(data, mep, muleContext);
//    }

    public static MuleEventContext getTestEventContext(Object data) throws Exception
    {
        return MuleTestUtils.getTestEventContext(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEventContext getTestEventContext(Object data, MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestEventContext(data, mep, muleContext);
    }

    public static Transformer getTestTransformer() throws Exception
    {
        return MuleTestUtils.getTestTransformer();
    }

    public static MuleEvent getTestEvent(Object data, InboundEndpoint endpoint) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, endpoint, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, Service service, InboundEndpoint endpoint)
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

    public static Service getTestService(String name, Class<?> clazz) throws Exception
    {
        return MuleTestUtils.getTestService(name, clazz, muleContext);
    }

    public static Service getTestService(String name, Class<?> clazz, Map<?, ?> props) throws Exception
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
        private Set<String> registeredTestMethod = new HashSet<String>();

        public TestInfo(TestCase test)
        {
            this.name = test.getClass().getName();

            // load test exclusions
            try
            {
                // We find the physical classpath root URL of the test class and
                // use that to find the correct resource. Works fine everywhere,
                // regardless of classloaders. See MULE-2414
                URL classUrl = ClassUtils.getClassPathRoot(test.getClass());
                URLClassLoader tempClassLoader = new URLClassLoader(new URL[]{classUrl});
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
                        excluded = IteratorUtils.filteredIterator(lines, new Predicate()
                        {
                            public boolean evaluate(Object object)
                            {
                                return StringUtils.equals(name, StringUtils.trimToEmpty((String) object));
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
        }

        public int getTestCount()
        {
            return testCount;
        }

        public synchronized void incTestCount(String testName)
        {
            if (!registeredTestMethod.contains(testName))
            {
                testCount++;
                registeredTestMethod.add(testName);
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

        @Override
        public synchronized String toString()
        {
            StringBuilder buf = new StringBuilder();
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

    /**
     * Determines if the test case should perform graceful shutdown or not.
     * Default is false so that tests run more quickly.
     */
    protected boolean isGracefulShutdown()
    {
        return false;
    }

    /**
     * Create an object of instance <code>clazz</code>. It will then register the object with the registry so that any
     * dependencies are injected and then the object will be initialised.
     * Note that if the object needs to be configured with additional state that cannot be passed into the constructor you should
     * create an instance first set any additional data on the object then call {@link #initialiseObject(Object)}.
     *
     * @param clazz the class to create an instance of.
     * @param <T>   Object of this type will be returned
     * @return an initialised instance of <code>class</code>
     * @throws Exception if there is a problem creating or initializing the object
     */
    protected <T extends Object> T createObject(Class<T> clazz) throws Exception
    {
        return createObject(clazz, ClassUtils.NO_ARGS);
    }

    /**
     * Create an object of instance <code>clazz</code>. It will then register the object with the registry so that any
     * dependencies are injected and then the object will be initialised.
     * Note that if the object needs to be configured with additional state that cannot be passed into the constructor you should
     * create an instance first set any additional data on the object then call {@link #initialiseObject(Object)}.
     *
     * @param clazz the class to create an instance of.
     * @param args  constructor parameters
     * @param <T>   Object of this type will be returned
     * @return an initialised instance of <code>class</code>
     * @throws Exception if there is a problem creating or initializing the object
     */
    @SuppressWarnings("unchecked")
    protected <T extends Object> T createObject(Class<T> clazz, Object... args) throws Exception
    {
        if (args == null)
        {
            args = ClassUtils.NO_ARGS;
        }
        Object o = ClassUtils.instanciateClass(clazz, args);
        muleContext.getRegistry().registerObject(String.valueOf(o.hashCode()), o);
        return (T) o;
    }

    /**
     * A convenience method that will register an object in the registry using its hashcode as the key.  This will cause the object
     * to have any objects injected and lifecycle methods called.  Note that the object lifecycle will be called to the same current
     * lifecycle as the MuleContext
     *
     * @param o the object to register and initialize it
     * @throws RegistrationException
     */
    protected void initialiseObject(Object o) throws RegistrationException
    {
        muleContext.getRegistry().registerObject(String.valueOf(o.hashCode()), o);
    }

    public SensingNullMessageProcessor getSensingNullMessageProcessor()
    {
        return new SensingNullMessageProcessor();
    }

    public TriggerableMessageSource getTriggerableMessageSource(MessageProcessor listener)
    {
        return new TriggerableMessageSource(listener);
    }

    public TriggerableMessageSource getTriggerableMessageSource()
    {
        return new TriggerableMessageSource();
    }

    /**
     * Define the ports as java system properties, starting with 'port1'
     */
    private void setPortProperties()
    {
        for (int i = 0; i < ports.size(); i++)
        {
            System.setProperty("port" + (i + 1), String.valueOf(ports.get(i)));
        }
    }

    public List<Integer> getPorts()
    {
        return ports;
    }
}
