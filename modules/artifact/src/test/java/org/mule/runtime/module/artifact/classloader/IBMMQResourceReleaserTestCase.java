/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.core.api.util.ClassUtils.getField;
import static org.mule.runtime.core.api.util.ClassUtils.getStaticFieldValue;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.getAllStackTraces;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.ThreadUtils.getAllThreads;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LEAK_PREVENTION)
@RunWith(Parameterized.class)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class IBMMQResourceReleaserTestCase extends AbstractMuleTestCase {

  static final String KNOWN_DRIVER_CLASS_NAME = "com.ibm.mq.jms.MQConnectionFactory";
  private final static String IBM_MQ_TRACE_CLASS = "com.ibm.msg.client.commonservices.trace.Trace";
  private final static String JUL_KNOWN_LEVEL_CLASS = "java.util.logging.Level$KnownLevel";
  private final static String IBM_MQ_COMMON_SERVICES_CLASS = "com.ibm.mq.internal.MQCommonServices";
  private final static String IBM_MQ_ENVIRONMENT_CLASS = "com.ibm.mq.MQEnvironment";
  private final static String IBM_MQ_JMS_TLS_CLASS = "com.ibm.msg.client.jms.internal.JmsTls";
  private final static String THREADLOCALS_FIELD = "threadLocals";
  private final static String INHERITABLE_THREADLOCALS_FIELD = "inheritableThreadLocals";
  private final static String THREADLOCAL_MAP_TABLE_CLASS = "java.lang.ThreadLocal$ThreadLocalMap";
  private final static String DRIVER_GROUP_ID = "com.ibm.mq";
  private final static String DRIVER_ARTIFACT_ID = "com.ibm.mq.allclient";
  private final static String IBM_MQ_MBEAN_DOMAIN = "IBM MQ";
  private final static String IBM_WORKER_CLASS = "com.ibm.msg.client.commonservices.workqueue.WorkQueueManager";
  private static final String JMSCC_THREAD_POOL_MAIN_NAME = "JMSCCThreadPoolMaster";

  String driverVersion;
  private final ClassLoaderLookupPolicy testLookupPolicy;
  MuleArtifactClassLoader artifactClassLoader = null;


  // Parameterized
  public IBMMQResourceReleaserTestCase(String driverVersion) {
    this.driverVersion = driverVersion;
    this.testLookupPolicy = new ClassLoaderLookupPolicy() {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        return CHILD_FIRST;
      }

      @Override
      public LookupStrategy getPackageLookupStrategy(String packageName) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
        return null;
      }
    };
  }


  @Parameterized.Parameters(name = "Testing Driver {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "9.2.5.0",
        "9.2.4.0",
        "9.2.3.0",
        "9.2.2.0",
        "9.1.1.0"
    };
  }


  @Before
  public void setup() throws Exception {
    Properties props = System.getProperties();
    props.remove("avoid.ibm.mq.cleanup");
    props.remove("avoid.ibm.mq.cleanup.mbeans");

    artifactClassLoader = getArtifactClassLoader("IBMMQResourceReleaserTestCase", currentThread().getContextClassLoader());

    // Force to load a Driver class so the resource releaser is flagged to run on dispose
    Class<?> connectionFactoryClass = Class.forName(KNOWN_DRIVER_CLASS_NAME, true, artifactClassLoader);
    Object connectionFactory = connectionFactoryClass.newInstance();

    createWorkerThread(artifactClassLoader);

    artifactClassLoader.dispose();
  }

  @Test
  @Description("When removing an application using the IBM MQ Driver, if JDK8, the KnownLevels references should be properly cleaned up")
  public void julKnownLevelsCleanupTest() throws Exception {
    /*
     * This only applies to JDK8. https://bugs.openjdk.java.net/browse/JDK-6543126
     * https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/jdk/src/share/classes/java/util/logging/Level.java#L534 *
     * https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/src/java.logging/share/classes/java/util/logging/Level.java#L563
     */
    assumeThat(System.getProperty("java.specification.version"), is(equalTo("1.8")));
    assertThat(countJULKnownLevels(artifactClassLoader), is(0));
  }

  @Test
  @Description("When removing an application using the IBM MQ Driver, the JmquiEnv reference from traceController should be properly cleaned up.")
  public void jmqiEnvCleanupTest() throws Exception {
    assertThat(getJmqiEnv(artifactClassLoader), is(nullValue()));
  }

  @Test
  @Description("When removing an application using the IBM MQ Driver, the JmsTlsClass should be properly cleaned up")
  public void mqCxManagerCleanupTest() throws Exception {
    assertThat(getDefaultMQCxManager(artifactClassLoader), is(nullValue()));
  }

  @Test
  @Description("When removing an application using the IBM MQ Driver, the JmsTls Class should be properly cleaned up.")
  public void jmsTlsCleanupTest() throws Exception {
    assertThat(getMyInstanceFromJmsTls(artifactClassLoader), is(nullValue()));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, the TracesController bean should be removed")
  public void traceControllerTest() throws Exception {
    assertThat(getTraceController(artifactClassLoader), is(nullValue()));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, there should not be threadLocal references left")
  public void threadLocalsTest() throws Exception {
    assertThat(countThreadLocals(artifactClassLoader), is(0));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, there should not be mbeans references registered")
  public void mBeansTest() throws Exception {
    assertThat(countMBeans(artifactClassLoader), is(0));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, there should not be worker thread references left")
  public void threadWorkerTestJustOneApplication() throws Exception {
    assertThat(countWorkerThreads(artifactClassLoader), is(0));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, there should not be worker thread references left")
  public void threadWorkerTestMultipleApplication() throws Exception {


    MuleArtifactClassLoader artifactClassLoaderApplication1 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase1", currentThread().getContextClassLoader());
    createWorkerThread(artifactClassLoaderApplication1);

    MuleArtifactClassLoader artifactClassLoaderApplication2 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase2", currentThread().getContextClassLoader());
    createWorkerThread(artifactClassLoaderApplication2);

    MuleArtifactClassLoader artifactClassLoaderApplication3 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase3", currentThread().getContextClassLoader());
    createWorkerThread(artifactClassLoaderApplication3);

    MuleArtifactClassLoader artifactClassLoaderApplication4 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase4", currentThread().getContextClassLoader());
    createWorkerThread(artifactClassLoaderApplication4);

    artifactClassLoaderApplication1.dispose();

    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(1));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(1));
    assertThat(countWorkerThreads(artifactClassLoaderApplication4), is(1));

    artifactClassLoaderApplication2.dispose();

    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(1));
    assertThat(countWorkerThreads(artifactClassLoaderApplication4), is(1));

    artifactClassLoaderApplication3.dispose();

    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication4), is(1));

    artifactClassLoaderApplication4.dispose();

    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication4), is(0));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, there should not be worker thread references left")
  public void threadWorkerApplicationDriver() throws Exception {
    MuleArtifactClassLoader muleDomainClassLoader =
        getArtifactClassLoader("muleDomainClassLoader", currentThread().getContextClassLoader());

    MuleArtifactClassLoader artifactClassLoaderApplication1 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase1", muleDomainClassLoader);
    createWorkerThread(artifactClassLoaderApplication1);

    MuleArtifactClassLoader artifactClassLoaderApplication2 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase2", muleDomainClassLoader);

    createWorkerThread(artifactClassLoaderApplication2);

    MuleArtifactClassLoader artifactClassLoaderApplication3 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase3", muleDomainClassLoader);

    createWorkerThread(artifactClassLoaderApplication3);

    assertThat(countWorkerThreads(muleDomainClassLoader), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(1));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(1));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(1));

    artifactClassLoaderApplication1.dispose();

    assertThat(countWorkerThreads(muleDomainClassLoader), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(1));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(1));

    artifactClassLoaderApplication2.dispose();

    assertThat(countWorkerThreads(muleDomainClassLoader), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(1));

    artifactClassLoaderApplication3.dispose();

    assertThat(countWorkerThreads(muleDomainClassLoader), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(0));
  }

  @Test
  @Description("When removing an application which contains the IBM MQ Driver, there should not be worker thread references left")
  public void threadWorkerDomainDriver() throws Exception {
    MuleArtifactClassLoader muleDomainClassLoader =
        getArtifactClassLoader("muleDomainClassLoader", currentThread().getContextClassLoader());

    // Creating Worker Thread domain level
    createWorkerThread(muleDomainClassLoader);

    MuleArtifactClassLoader artifactClassLoaderApplication1 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase1", currentThread().getContextClassLoader());
    MuleArtifactClassLoader artifactClassLoaderApplication2 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase2", currentThread().getContextClassLoader());
    MuleArtifactClassLoader artifactClassLoaderApplication3 =
        getArtifactClassLoader("IBMMQResourceReleaserTestCase3", currentThread().getContextClassLoader());

    assertThat(countWorkerThreads(muleDomainClassLoader), is(1));

    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(0));

    artifactClassLoaderApplication1.dispose();
    assertThat(countWorkerThreads(muleDomainClassLoader), is(1));

    muleDomainClassLoader.dispose();
    assertThat(countWorkerThreads(muleDomainClassLoader), is(0));

    assertThat(countWorkerThreads(artifactClassLoaderApplication1), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication2), is(0));
    assertThat(countWorkerThreads(artifactClassLoaderApplication3), is(0));
  }

  private int countMBeans(MuleArtifactClassLoader artifactClassLoader) throws MalformedObjectNameException {
    MBeanServer mBeanServer = getPlatformMBeanServer();
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", "CommonServices");
    keys.put("name", "*");
    return mBeanServer.queryMBeans(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys), null).size();
  }

  private int countThreadLocals(ClassLoader artifactClassLoader) throws Exception {
    int counter = 0;

    Field threadLocalsField = getField(Thread.class, THREADLOCALS_FIELD, false);
    threadLocalsField.setAccessible(true);
    Field inheritableThreadLocalsField = getField(Thread.class, INHERITABLE_THREADLOCALS_FIELD, false);

    inheritableThreadLocalsField.setAccessible(true);

    Class<?> threadLocalMapTableClass = loadClass(THREADLOCAL_MAP_TABLE_CLASS, artifactClassLoader);
    Field threadLocalMapTableField = getField(threadLocalMapTableClass, "table", false);
    threadLocalMapTableField.setAccessible(true);
    for (Thread thread : getAllStackTraces().keySet()) {
      counter += countThreadLocalsInMap(artifactClassLoader, threadLocalMapTableField, threadLocalsField.get(thread));
      counter += countThreadLocalsInMap(artifactClassLoader, threadLocalMapTableField, inheritableThreadLocalsField.get(thread));
    }
    return counter;
  }

  private int countThreadLocalsInMap(ClassLoader artifactClassLoader, Field threadLocalMapTableField, Object threadLocalMap)
      throws Exception {

    if (threadLocalMap == null) {
      return 0;
    }

    int counter = 0;
    Object[] threadLocalMapTable = {};
    threadLocalMapTable = (Object[]) threadLocalMapTableField.get(threadLocalMap);

    for (Object entry : threadLocalMapTable) {
      if (entry != null) {
        Reference<?> reference = (Reference<?>) entry;
        ThreadLocal<?> threadLocal = (ThreadLocal<?>) reference.get();
        if (threadLocal != null) {
          Object x = threadLocal.get();
          if (x != null) {
            if (artifactClassLoader == x.getClass().getClassLoader()) {
              counter++;
            }
          }
        }
      }
    }
    return counter;
  }


  private Object getMyInstanceFromJmsTls(ClassLoader artifactClassLoader) throws Exception {
    Class<?> jmsTlsClass = loadClass(IBM_MQ_JMS_TLS_CLASS, artifactClassLoader);
    Field myInstanceField = getField(jmsTlsClass, "myInstance", false);
    myInstanceField.setAccessible(true);
    return myInstanceField.get(null);
  }

  private Object getDefaultMQCxManager(ClassLoader artifactClassLoader) throws Exception {
    Class<?> mqEnvironmentClass = loadClass(IBM_MQ_ENVIRONMENT_CLASS, artifactClassLoader);
    Field defaultMQCxManagerField = getField(mqEnvironmentClass, "defaultMQCxManager", false);
    defaultMQCxManagerField.setAccessible(true);
    return defaultMQCxManagerField.get(null);
  }

  private Object getJmqiEnv(ClassLoader artifactClassLoader) throws Exception {
    Class<?> commonServicesClass = loadClass(IBM_MQ_COMMON_SERVICES_CLASS, artifactClassLoader);
    Field jmqiEnvField = getField(commonServicesClass, "jmqiEnv", false);
    jmqiEnvField.setAccessible(true);
    return jmqiEnvField.get(null);
  }

  private Object getTraceController(ClassLoader artifactClassLoader) throws Exception {
    Class<?> ibmMQTraceClass = loadClass(IBM_MQ_TRACE_CLASS, artifactClassLoader);
    Field traceControllerField = getField(ibmMQTraceClass, "traceController", false);
    traceControllerField.setAccessible(true);
    return traceControllerField.get(null);
  }

  private int countJULKnownLevels(ClassLoader artifactClassLoader) throws Exception {
    int counter = 0;
    Class<?> knownLevelClass = loadClass(JUL_KNOWN_LEVEL_CLASS, artifactClassLoader);
    Field levelObjectField = null;
    try {
      levelObjectField = getField(knownLevelClass, "levelObject", false);
    } catch (NoSuchFieldException ex) {
      throw ex;
    }
    levelObjectField.setAccessible(true);
    Map<?, List> nameToLevels = getStaticFieldValue(knownLevelClass, "nameToLevels", false);
    Map<?, List> intToLevels = getStaticFieldValue(knownLevelClass, "intToLevels", false);
    if (nameToLevels != null) {
      for (List knownLevels : nameToLevels.values()) {
        for (Iterator iter = knownLevels.listIterator(); iter.hasNext();) {
          Object knownLevel = iter.next();
          Level levelObject = (Level) levelObjectField.get(knownLevel);
          if (artifactClassLoader == levelObject.getClass().getClassLoader()) {
            counter++;
          }
        }
      }

    }
    if (intToLevels != null) {
      for (List knownLevels : intToLevels.values()) {
        for (Iterator iter = knownLevels.listIterator(); iter.hasNext();) {
          Object knownLevel = iter.next();
          Level levelObject = (Level) levelObjectField.get(knownLevel);
          if (artifactClassLoader == levelObject.getClass().getClassLoader()) {
            counter++;
          }
        }
      }
    }
    return counter;
  }

  private int countWorkerThreads(ClassLoader artifactClassLoader) {
    List<Thread> threads = getAllThreads().stream()
        .filter(thread -> thread.getName().equals(JMSCC_THREAD_POOL_MAIN_NAME))
        .filter(thread -> thread.getClass().getClassLoader() == artifactClassLoader)
        .collect(Collectors.toList());

    return threads.size();
  }

  private void createWorkerThread(MuleArtifactClassLoader artifactClassLoader) throws ClassNotFoundException {
    Class.forName(IBM_WORKER_CLASS, true, artifactClassLoader);

    // Verify if the worker thread is alive.
    assertThat(countWorkerThreads(artifactClassLoader), is(1));
  }

  private BundleDependency getBundleDependency() {
    URL settingsUrl = getClass().getClassLoader().getResource("custom-settings.xml");
    final MavenClientProvider mavenClientProvider = discoverProvider(this.getClass().getClassLoader());

    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
        newMavenConfigurationBuilder().globalSettingsLocation(toFile(settingsUrl));

    MavenClient mavenClient = mavenClientProvider
        .createMavenClient(mavenConfigurationBuilder.localMavenRepositoryLocation(localMavenRepository.get()).build());

    BundleDescriptor bundleDescriptor = new BundleDescriptor.Builder().setGroupId(DRIVER_GROUP_ID)
        .setArtifactId(DRIVER_ARTIFACT_ID).setVersion(driverVersion).build();

    BundleDependency dependency = mavenClient.resolveBundleDescriptor(bundleDescriptor);
    return dependency;
  }

  private MuleArtifactClassLoader getArtifactClassLoader(String artifactId, ClassLoader classLoader)
      throws MalformedURLException {
    return new MuleArtifactClassLoader(artifactId,
                                       mock(ArtifactDescriptor.class),
                                       new URL[] {getBundleDependency().getBundleUri().toURL()},
                                       classLoader,
                                       testLookupPolicy);
  }
}
