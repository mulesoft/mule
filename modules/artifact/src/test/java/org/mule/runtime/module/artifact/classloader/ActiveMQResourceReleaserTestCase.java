/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;
import static org.mule.runtime.module.artifact.classloader.DependencyResolver.getDependencyFromMaven;
import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.enumerate;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import org.mule.module.artifact.classloader.ActiveMQResourceReleaser;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.MulePluginClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
@RunWith(Parameterized.class)
public class ActiveMQResourceReleaserTestCase extends AbstractMuleTestCase {

  private final static String DRIVER_ARTIFACT_ID = "activemq-all";
  private final static String DRIVER_GROUP_ID = "org.apache.activemq";
  static final String DRIVER_CLASS_NAME = "org.apache.activemq.ActiveMQConnectionFactory";
  private static final String ACTIVEMQ_DRIVER_TIMER_THREAD_NAME = "ActiveMQ InactivityMonitor ReadCheckTimer";
  private final static String ACTIVEMQ_URL_CONFIG =
      "failover:(tcp://192.168.1.111:61616)?jms.useAsyncSend=true&initialReconnectDelay=1000&maxReconnectAttempts=-1";

  String driverVersion;
  private final ClassLoaderLookupPolicy testLookupPolicy;
  MulePluginClassLoader artifactClassLoader = null;

  public ActiveMQResourceReleaserTestCase(String driverVersion) {
    this.driverVersion = driverVersion;
    this.testLookupPolicy = CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
  }

  @Parameterized.Parameters(name = "Testing Driver {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "5.16.4"
    };
  }

  @Before
  public void setup() throws Exception {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), is(false));

    artifactClassLoader =
        new MulePluginClassLoader("ActiveMQResourceReleaserTestCase",
                                  mock(ArtifactDescriptor.class),
                                  new URL[] {getDependencyFromMaven(DRIVER_GROUP_ID, DRIVER_ARTIFACT_ID, driverVersion)},
                                  currentThread().getContextClassLoader(),
                                  testLookupPolicy);

    CompositeClassLoader classLoader = from(artifactClassLoader);
    currentThread().setContextClassLoader(classLoader);
  }

  @Test
  public void checkIfActiveMQResourceReleaserInterruptAbstractInactivityMonitorThread() throws ReflectiveOperationException {
    startActiveMQConnectionThread();
    artifactClassLoader.dispose();
    assertFalse(getNameListOfActiveThreads().contains(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME));
  }

  @Test
  public void checkIfActiveMQResourceReleaserCanBeInvokedManyTimes() throws ReflectiveOperationException {
    startActiveMQConnectionThread();
    new ActiveMQResourceReleaser(artifactClassLoader).release();
    new ActiveMQResourceReleaser(artifactClassLoader).release();
    artifactClassLoader.dispose();
    assertFalse(getNameListOfActiveThreads().contains(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME));
  }

  private void startActiveMQConnectionThread() throws ReflectiveOperationException {
    Class<?> activeMqFactoryClass = Class.forName(DRIVER_CLASS_NAME, true, artifactClassLoader);
    Object activeMpFactoryObject = activeMqFactoryClass.getDeclaredConstructor(String.class).newInstance(ACTIVEMQ_URL_CONFIG);

    Object conObj = activeMqFactoryClass.getMethod("createConnection").invoke(activeMpFactoryObject);
    Class<?> connection = conObj.getClass();

    Method startActiveMQConn = connection.getMethod("start");
    ActiveMQResourceReleaserThreadUtil runnable = new ActiveMQResourceReleaserThreadUtil(startActiveMQConn, conObj);
    Thread activeMQConnectionThread = new Thread(runnable, "ActiveMQConnectionThread");
    activeMQConnectionThread.start();

    await().atMost(1, TimeUnit.SECONDS).until(listOfThreadsContainInactivityMonitorThread());
  }

  private Callable<Boolean> listOfThreadsContainInactivityMonitorThread() {
    return () -> getNameListOfActiveThreads().contains(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME);
  }

  private List<String> getNameListOfActiveThreads() {
    Thread[] threads = new Thread[activeCount()];
    enumerate(threads);
    List<String> activeThreadNames = new ArrayList<>();
    for (Thread thread : threads) {
      Optional<String> actualName = Optional.ofNullable(thread.getName());
      actualName.ifPresent(activeThreadNames::add);
    }
    return activeThreadNames;
  }
}
