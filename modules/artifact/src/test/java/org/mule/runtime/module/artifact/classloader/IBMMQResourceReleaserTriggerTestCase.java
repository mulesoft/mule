/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.core.api.util.ClassUtils.getField;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.module.artifact.classloader.DependencyResolver.getDependencyFromMaven;
import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Thread.currentThread;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.MulePluginClassLoader;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
@RunWith(Parameterized.class)
public class IBMMQResourceReleaserTriggerTestCase {

  static final String KNOWN_DRIVER_CLASS_NAME = "com.ibm.mq.jms.MQConnectionFactory";
  private final static String IBM_MQ_TRACE_CLASS = "com.ibm.msg.client.commonservices.trace.Trace";
  private final static String DRIVER_GROUP_ID = "com.ibm.mq";
  private final static String DRIVER_ARTIFACT_ID = "com.ibm.mq.allclient";
  private final static String IBM_MQ_MBEAN_DOMAIN = "IBM MQ";

  String driverVersion;
  MulePluginClassLoader artifactClassLoader = null;

  // Parameterized
  public IBMMQResourceReleaserTriggerTestCase(String driverVersion) {
    this.driverVersion = driverVersion;
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
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), Is.is(false));

    Properties props = System.getProperties();
    props.remove("avoid.ibm.mq.cleanup");
    props.remove("avoid.ibm.mq.cleanup.mbeans");

    artifactClassLoader =
        new MulePluginClassLoader("IBMMQResourceReleaserTriggerTestCase",
                                  mock(ArtifactDescriptor.class),
                                  new URL[] {getDependencyFromMaven(DRIVER_GROUP_ID, DRIVER_ARTIFACT_ID, driverVersion)},
                                  currentThread().getContextClassLoader(),
                                  CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY);
  }


  @Test
  @Description("When redeploying an application which contains the IBM MQ Driver, the proper cleanup should be performed on redeployment")
  public void releaserTriggerTest() throws Exception {
    // Driver not loaded yet. Should not cleanup on dispose.
    Field shouldReleaseIbmMQResourcesField = getField(MuleArtifactClassLoader.class, "shouldReleaseIbmMQResources", false);
    shouldReleaseIbmMQResourcesField.setAccessible(true);
    assertThat(shouldReleaseIbmMQResourcesField.get(artifactClassLoader), is(false));
    // Force to load a Driver class so the resource releaser is flagged to run on dispose
    Class<?> connectionFactoryClass = Class.forName(KNOWN_DRIVER_CLASS_NAME, true, artifactClassLoader);
    Object connectionFactory = connectionFactoryClass.newInstance();
    Class<?> traceClass = Class.forName("com.ibm.msg.client.commonservices.trace.Trace", true, artifactClassLoader);
    // Driver loaded... should clean on dispose.
    assertThat(shouldReleaseIbmMQResourcesField.get(artifactClassLoader), is(true));
    // TraceController is not null
    Class<?> ibmMQTraceClass = loadClass(IBM_MQ_TRACE_CLASS, artifactClassLoader);
    Field traceControllerField = getField(ibmMQTraceClass, "traceController", false);
    traceControllerField.setAccessible(true);
    assertThat(traceControllerField.get(null), is(notNullValue()));
    artifactClassLoader.dispose();
  }

  @Test
  @Description("When redeploying an application which contains the IBM MQ Driver, the proper cleanup should clean mbeans")
  public void releaserMBeansPropertyFalseTriggerTest() throws Exception {
    Field shouldReleaseIbmMQResourcesField = getField(MuleArtifactClassLoader.class, "shouldReleaseIbmMQResources", false);
    shouldReleaseIbmMQResourcesField.setAccessible(true);
    assertThat(shouldReleaseIbmMQResourcesField.get(artifactClassLoader), is(false));
    // Force to load a Driver class so the resource releaser is flagged to run on dispose
    Class<?> connectionFactoryClass = Class.forName(KNOWN_DRIVER_CLASS_NAME, true, artifactClassLoader);
    Object connectionFactory = connectionFactoryClass.newInstance();
    artifactClassLoader.dispose();
    assertThat(countMBeans(artifactClassLoader), is(0));
  }

  @Test
  @Description("When redeploying an application which contains the IBM MQ Driver, the proper cleanup should be performed " +
      "on redeployment but, if the property avoid.ibm.mq.cleanup.mbeans=true, the mbeans clean should be skiped.")
  public void releaserMBeansPropertyTrueTriggerTest() throws Exception {
    Field shouldReleaseIbmMQResourcesField = getField(MuleArtifactClassLoader.class, "shouldReleaseIbmMQResources", false);
    shouldReleaseIbmMQResourcesField.setAccessible(true);
    assertThat(shouldReleaseIbmMQResourcesField.get(artifactClassLoader), is(false));
    // Force to load a Driver class so the resource releaser is flagged to run on dispose
    Class<?> connectionFactoryClass = Class.forName(KNOWN_DRIVER_CLASS_NAME, true, artifactClassLoader);
    Object connectionFactory = connectionFactoryClass.newInstance();
    Properties props = System.getProperties();
    props.setProperty("avoid.ibm.mq.cleanup.mbeans", "true");
    int countMBeans = countMBeans(artifactClassLoader);
    artifactClassLoader.dispose();
    assertThat(countMBeans(artifactClassLoader), is(countMBeans));
  }

  private int countMBeans(MuleArtifactClassLoader artifactClassLoader) throws MalformedObjectNameException {
    MBeanServer mBeanServer = getPlatformMBeanServer();
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", "CommonServices");
    keys.put("name", "*");
    return mBeanServer.queryMBeans(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys), null).size();
  }

  @After
  public void cleanUp() throws Exception {
    Properties props = System.getProperties();
    props.remove("avoid.ibm.mq.cleanup");
    props.remove("avoid.ibm.mq.cleanup.mbeans");
  }
}
