/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Thread.*;
import static org.apache.commons.io.FileUtils.toFile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

@Feature(LEAK_PREVENTION)
@RunWith(Parameterized.class)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class JMSResourceReleaserTestCase extends AbstractMuleTestCase {

  private final static String DRIVER_ARTIFACT_ID = "activemq-all";
  private static final String TEST_CLASSLOADER_ARTIFACT_ID = "test";
  private final static String DRIVER_GROUP_ID = "org.apache.activemq";
  static final String DRIVER_CLASS_NAME = "org.apache.activemq.ActiveMQConnectionFactory";
  private static final String ACTIVEMQ_DRIVER_TIMER_THREAD_NAME = "ActiveMQ InactivityMonitor ReadCheckTimer";
  private final static String ACTIVEMQ_URL_CONFIG =
          "failover:(tcp://192.168.1.111:61616)?jms.useAsyncSend=true&initialReconnectDelay=1000&maxReconnectAttempts=-1";

  String driverVersion;
  private final ClassLoaderLookupPolicy testLookupPolicy;
  MuleArtifactClassLoader artifactClassLoader = null;

  public JMSResourceReleaserTestCase(String driverVersion) {
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
      public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy, boolean overwrite) {
        return null;
      }
    };
  }

  @Parameterized.Parameters(name = "Testing Driver {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "5.16.4"
    };
  }

  @Before
  public void setup() throws Exception {

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

    artifactClassLoader =
        new MuleArtifactClassLoader(TEST_CLASSLOADER_ARTIFACT_ID, mock(ArtifactDescriptor.class),
                                    new URL[] {dependency.getBundleUri().toURL()},
                                    currentThread().getContextClassLoader(), testLookupPolicy);
  }


  @Test
  public void checkIfJMSResourceReleaserInterruptAbstractInactivityMonitorThread() throws InterruptedException,
      ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

    Class<?> activeMqFactoryClass = Class.forName(DRIVER_CLASS_NAME, true, artifactClassLoader);
    Object activeMpFactoryObject = activeMqFactoryClass.getDeclaredConstructor(String.class).newInstance(ACTIVEMQ_URL_CONFIG);

    Object conObj = activeMqFactoryClass.getMethod("createConnection").invoke(activeMpFactoryObject);
    Class<?> connection = conObj.getClass();

    Method startActiveMQConn = connection.getMethod("start");
    JMSReleaserThreadUtil runnable = new JMSReleaserThreadUtil(startActiveMQConn, conObj);
    Thread activeMQConnectionThread = new Thread(runnable, "ActiveMQConnectionThread");
    activeMQConnectionThread.start();

    Thread.sleep(200);
    assertTrue(getNameListOfActiveThreads().contains(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME));
    artifactClassLoader.dispose();
    assertFalse(getNameListOfActiveThreads().contains(ACTIVEMQ_DRIVER_TIMER_THREAD_NAME));
  }

  ///////////////////
  // PRIVATE-METHODS//
  ///////////////////

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
