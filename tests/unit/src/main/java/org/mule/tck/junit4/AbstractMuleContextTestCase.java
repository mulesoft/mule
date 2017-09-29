/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.junit4.TestsLogConfigurationHelper.clearLoggingConfig;
import static org.mule.tck.junit4.TestsLogConfigurationHelper.configureLoggingForTest;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.el.ExpressionExecutor;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.runtime.http.api.HttpService;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.config.TestServicesConfigurationBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

/**
 * Extends {@link AbstractMuleTestCase} providing access to a {@link MuleContext} instance and tools for manage it.
 */
public abstract class AbstractMuleContextTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(AbstractMuleContextTestCase.class);
  public static final String WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY = "workingDirectory";

  public TestServicesConfigurationBuilder testServicesConfigurationBuilder;
  public Supplier<TestServicesConfigurationBuilder> testServicesConfigurationBuilderSupplier =
      () -> new TestServicesConfigurationBuilder(mockHttpService(), mockExprExecutorService());

  public TemporaryFolder workingDirectory = new TemporaryFolder();

  /**
   * Top-level directories under <code>.mule</code> which are not deleted on each test case recycle. This is required, e.g. to
   * play nice with transaction manager recovery service object store.
   */
  public static final String[] IGNORED_DOT_MULE_DIRS = new String[] {"transaction-log"};

  /**
   * The context used to run this test. Context will be created per class or per method depending on
   * {@link #disposeContextPerClass}. The context will be started only when {@link #startContext} is true.
   */
  protected static MuleContext muleContext;

  /**
   * Start the muleContext once it's configured (defaults to false for AbstractMuleTestCase, true for FunctionalTestCase).
   */
  private boolean startContext = false;

  /**
   * Convenient test message for unit testing.
   */
  public static final String TEST_MESSAGE = "Test Message";

  /**
   * Default timeout for multithreaded tests (using CountDownLatch, WaitableBoolean, etc.), in milliseconds. The higher this
   * value, the more reliable the test will be, so it should be set high for Continuous Integration. However, this can waste time
   * during day-to-day development cycles, so you may want to temporarily lower it while debugging.
   */
  public static final long LOCK_TIMEOUT = 30000;

  /**
   * Default timeout for waiting for responses
   */
  public static final int RECEIVE_TIMEOUT = 5000;

  /**
   * Default timeout used when blocking on {@link org.reactivestreams.Publisher} completion.
   */
  public static final int BLOCK_TIMEOUT = 500;

  /**
   * Indicates if the context should be instantiated per context. Default is false, which means that a context will be
   * instantiated per test method.
   */
  private boolean disposeContextPerClass;
  private static boolean logConfigured;

  protected ConfigurationComponentLocator componentLocator = mock(ConfigurationComponentLocator.class, RETURNS_DEEP_STUBS);

  protected boolean isDisposeContextPerClass() {
    return disposeContextPerClass;
  }

  protected void setDisposeContextPerClass(boolean val) {
    disposeContextPerClass = val;
  }

  @Before
  public final void setUpMuleContext() throws Exception {
    if (!logConfigured) {
      configureLoggingForTest(getClass());
      logConfigured = true;
    }
    workingDirectory.create();
    String workingDirectoryOldValue =
        System.setProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY, workingDirectory.getRoot().getAbsolutePath());
    try {
      doSetUpBeforeMuleContextCreation();

      muleContext = createMuleContext();

      if (doTestClassInjection()) {
        muleContext.getInjector().inject(this);
      }

      if (isStartContext() && muleContext != null && !muleContext.isStarted()) {
        startMuleContext();
      }

      doSetUp();
    } finally {
      if (workingDirectoryOldValue != null) {
        System.setProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY, workingDirectoryOldValue);
      } else {
        System.clearProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY);
      }
    }
  }

  protected void doSetUpBeforeMuleContextCreation() throws Exception {}

  private void startMuleContext() throws MuleException, InterruptedException {
    final AtomicReference<Latch> contextStartedLatch = new AtomicReference<>();

    contextStartedLatch.set(new Latch());
    // Do not inline it, otherwise the type of the listener is lost
    final MuleContextNotificationListener<MuleContextNotification> listener =
        new MuleContextNotificationListener<MuleContextNotification>() {

          @Override
          public boolean isBlocking() {
            return false;
          }

          @Override
          public void onNotification(MuleContextNotification notification) {
            contextStartedLatch.get().countDown();
          }
        };
    muleContext.getNotificationManager().addListener(listener);

    muleContext.start();

    contextStartedLatch.get().await(20, SECONDS);
  }

  protected boolean doTestClassInjection() {
    return false;
  }

  /**
   * Enables the adding of extra behavior on the set up stage of a test right after the creation of the mule context in
   * {@link #setUpMuleContext}.
   * <p>
   * Under normal circumstances this method could be replaced by a <code>@Before</code> annotated method.
   *
   * @throws Exception if something fails that should halt the test case
   */
  protected void doSetUp() throws Exception {
    // template method
  }

  protected MuleContext createMuleContext() throws Exception {
    // Should we set up the manager for every method?
    MuleContext context;
    if (isDisposeContextPerClass() && muleContext != null) {
      context = muleContext;
    } else {
      final ClassLoader executionClassLoader = getExecutionClassLoader();
      final ClassLoader originalContextClassLoader = currentThread().getContextClassLoader();
      try {
        currentThread().setContextClassLoader(executionClassLoader);

        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        List<ConfigurationBuilder> builders = new ArrayList<>();
        builders.add(new SimpleConfigurationBuilder(getStartUpRegistryObjects()));
        addBuilders(builders);
        builders.add(getBuilder());
        MuleContextBuilder contextBuilder = MuleContextBuilder.builder(APP);
        DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
        String workingDirectory = this.workingDirectory.getRoot().getAbsolutePath();
        LOGGER.info("Using working directory for test: " + workingDirectory);
        muleConfiguration.setWorkingDirectory(workingDirectory);
        muleConfiguration.setId(this.getClass().getSimpleName() + "#" + name.getMethodName());
        contextBuilder.setMuleConfiguration(muleConfiguration);
        contextBuilder.setExecutionClassLoader(executionClassLoader);
        contextBuilder.setObjectSerializer(getObjectSerializer());
        configureMuleContext(contextBuilder);
        context = muleContextFactory.createMuleContext(builders, contextBuilder);
        recordSchedulersOnInit(context);
        if (!isGracefulShutdown()) {
          ((DefaultMuleConfiguration) context.getConfiguration()).setShutdownTimeout(0);
        }
      } finally {
        currentThread().setContextClassLoader(originalContextClassLoader);
      }
    }
    return context;
  }

  /**
   * @return the {@link ObjectSerializer} to use on the test's {@link MuleContext}
   */
  protected ObjectSerializer getObjectSerializer() {
    return new JavaObjectSerializer();
  }

  protected ClassLoader getExecutionClassLoader() {
    return this.getClass().getClassLoader();
  }

  // This shouldn't be needed by Test cases but can be used by base testcases that wish to add further builders when
  // creating the MuleContext.
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    testServicesConfigurationBuilder = testServicesConfigurationBuilderSupplier.get();
    builders.add(testServicesConfigurationBuilder);
  }

  /**
   * Defines if a mock should be used for the {@link HttpService}. If {@code false} an implementation will need to be provided.
   *
   * @return whether or not the {@link HttpService} should be mocked.
   */
  protected boolean mockHttpService() {
    return true;
  }

  /**
   * Defines if a mock should be used for the {@link ExpressionExecutor}. If {@code false} an implementation will need to be
   * provided.
   *
   * @return whether or not the {@link ExpressionExecutor} should be mocked.
   */
  protected boolean mockExprExecutorService() {
    return false;
  }

  /**
   * Override this method to set properties of the MuleContextBuilder before it is used to create the MuleContext.
   */
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {}

  protected ConfigurationBuilder getBuilder() throws Exception {
    return new DefaultsConfigurationBuilder();
  }

  protected String getConfigurationResources() {
    return StringUtils.EMPTY;
  }

  protected Map<String, Object> getStartUpRegistryObjects() {
    return emptyMap();
  }

  @After
  public final void disposeContextPerTest() throws Exception {
    doTearDown();

    if (!isDisposeContextPerClass()) {
      if (isStartContext() && muleContext != null && muleContext.isStarted()) {
        muleContext.stop();
      }

      disposeContext();
      if (testServicesConfigurationBuilder != null) {
        testServicesConfigurationBuilder.stopServices();
      }
      doTearDownAfterMuleContextDispose();
    }

    // When an Assumption fails then junit doesn't call @Before methods so we need to avoid
    // executing delete if there's no root folder.
    workingDirectory.delete();
  }

  protected void doTearDownAfterMuleContextDispose() throws Exception {}

  @AfterClass
  public static void disposeContext() throws MuleException {
    try {
      if (muleContext != null && !(muleContext.isDisposed() || muleContext.isDisposing())) {
        try {
          muleContext.dispose();
        } catch (IllegalStateException e) {
          // Ignore
          LOGGER.warn(e + " : " + e.getMessage());
        }

        verifyAndStopSchedulers();

        MuleConfiguration configuration = muleContext.getConfiguration();

        if (configuration != null) {
          final String workingDir = configuration.getWorkingDirectory();
          // do not delete TM recovery object store, everything else is good to
          // go
          deleteTree(newFile(workingDir), IGNORED_DOT_MULE_DIRS);
        }
      }
      deleteTree(newFile("./ActiveMQ"));
    } finally {
      muleContext = null;
      clearLoggingConfig();
    }
  }

  private static List<SchedulerView> schedulersOnInit;

  protected static void recordSchedulersOnInit(MuleContext context) {
    if (context != null) {
      final SchedulerService serviceImpl = context.getSchedulerService();
      schedulersOnInit = serviceImpl.getSchedulers();
    } else {
      schedulersOnInit = emptyList();
    }
  }

  protected static void verifyAndStopSchedulers() throws MuleException {
    final SchedulerService serviceImpl = muleContext.getSchedulerService();

    Set<String> schedulersOnInitNames = schedulersOnInit.stream().map(s -> s.getName()).collect(toSet());
    try {
      assertThat(muleContext.getSchedulerService().getSchedulers().stream()
          .filter(s -> !schedulersOnInitNames.contains(s.getName())).collect(toList()), empty());
    } finally {
      if (serviceImpl instanceof SimpleUnitTestSupportSchedulerService) {
        stopIfNeeded(serviceImpl);
      }
    }

  }

  /**
   * Enables the adding of extra behavior on the tear down stage of a test before the mule context is disposed in
   * {@link #disposeContextPerTest}.
   * <p>
   * Under normal circumstances this method could be replace with a <code>@After</code> annotated method.
   *
   * @throws Exception if something fails that should halt the testcase
   */
  protected void doTearDown() throws Exception {
    // template method
  }

  @Override
  protected <B extends CoreEvent.Builder> B getEventBuilder() throws MuleException {
    return (B) eventBuilder(muleContext);
  }

  protected boolean isStartContext() {
    return startContext;
  }

  protected void setStartContext(boolean startContext) {
    this.startContext = startContext;
  }

  /**
   * Determines if the test case should perform graceful shutdown or not. Default is false so that tests run more quickly.
   */
  protected boolean isGracefulShutdown() {
    return false;
  }

  public SensingNullMessageProcessor getSensingNullMessageProcessor() {
    return new SensingNullMessageProcessor();
  }

  public TriggerableMessageSource getTriggerableMessageSource() {
    return new TriggerableMessageSource();
  }

  /**
   * @return the mule application working directory where the app data is stored
   */
  protected File getWorkingDirectory() {
    return workingDirectory.getRoot();
  }

  /**
   * @param fileName name of the file. Can contain subfolders separated by {@link java.io.File#separator}
   * @return a File inside the working directory of the application.
   */
  protected File getFileInsideWorkingDirectory(String fileName) {
    return new File(getWorkingDirectory(), fileName);
  }

  /**
   * Uses {@link org.mule.runtime.api.transformation.TransformationService} to get a {@link String} representation of a message.
   *
   * @param message message to get payload from
   * @return String representation of the message payload
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected String getPayloadAsString(org.mule.runtime.api.message.Message message) throws Exception {
    return (String) getPayload(message, DataType.STRING);
  }

  /**
   * Uses {@link org.mule.runtime.api.transformation.TransformationService} to get byte[] representation of a message.
   *
   * @param message message to get payload from
   * @return byte[] representation of the message payload
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected byte[] getPayloadAsBytes(Message message) throws Exception {
    return (byte[]) getPayload(message, DataType.BYTE_ARRAY);
  }

  /**
   * Uses {@link org.mule.runtime.api.transformation.TransformationService} to get representation of a message for a given {@link DataType}
   *
   * @param message message to get payload from
   * @param dataType dataType to be transformed to
   * @return representation of the message payload of the required dataType
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected Object getPayload(Message message, DataType dataType) throws Exception {
    return muleContext.getTransformationService().transform(message, dataType).getPayload().getValue();
  }

  /**
   * Uses {@link org.mule.runtime.api.transformation.TransformationService} to get representation of a message for a given {@link Class}
   *
   * @param message message to get payload from
   * @param clazz type of the payload to be transformed to
   * @return representation of the message payload of the required class
   * @throws Exception if there is an unexpected error obtaining the payload representation
   */
  protected <T> T getPayload(Message message, Class<T> clazz) throws Exception {
    return (T) getPayload(message, DataType.fromType(clazz));
  }

  protected CoreEvent process(Processor processor, CoreEvent event) throws Exception {
    setMuleContextIfNeeded(processor, muleContext);
    return processor.process(event);
  }

  public static Map<QName, Object> getAppleFlowComponentLocationAnnotations() {
    return getFlowComponentLocationAnnotations(APPLE_FLOW);
  }

  protected static Map<QName, Object> getFlowComponentLocationAnnotations(String rootComponentName) {
    return ImmutableMap.<QName, Object>builder().put(LOCATION_KEY, fromSingleComponent(rootComponentName)).build();
  }

}
