/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.getAllStackTraces;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.util.ClassUtils.getField;
import static org.mule.runtime.core.api.util.ClassUtils.getStaticFieldValue;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;


@RunWith(Parameterized.class)
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


  String driverFile;
  private ClassLoaderLookupPolicy testLookupPolicy;


  //Parameterized
  public IBMMQResourceReleaserTestCase(String driverFile) {
    this.driverFile = driverFile;
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
        "ibm/mq/com.ibm.mq.allclient-9.2.3.0.jar",
        "ibm/mq/com.ibm.mq.allclient-9.2.2.0.jar",
        "ibm/mq/com.ibm.mq.allclient-9.1.1.0.jar"
    };
  }


  @Test
  public void cleanUpTest() throws Exception {
    try (MuleArtifactClassLoader artifactClassLoader =
        new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class),
                                    new URL[] {ClassUtils.getResource(driverFile, this.getClass())},
                                    currentThread().getContextClassLoader(), testLookupPolicy)) {

      //Driver not loaded yet. Should not cleanup on dispose.
      Field shouldReleaseIbmMQResourcesField = getField(MuleArtifactClassLoader.class, "shouldReleaseIbmMQResources", false);
      shouldReleaseIbmMQResourcesField.setAccessible(true);

      assertThat(shouldReleaseIbmMQResourcesField.get(artifactClassLoader), is(false));

      // Force to load a Driver class so the resource releaser is flagged to run on dispose
      Class<?> connectionFactoryClass = Class.forName(KNOWN_DRIVER_CLASS_NAME, true, artifactClassLoader);
      Object connectionFactory = connectionFactoryClass.newInstance();
      Class<?> traceClass = Class.forName("com.ibm.msg.client.commonservices.trace.Trace", true, artifactClassLoader);

      //Driver loaded... should clean on dispose.
      assertThat(shouldReleaseIbmMQResourcesField.get(artifactClassLoader), is(true));

      //TraceController is not null
      assertThat(getTraceController(artifactClassLoader), is(notNullValue()));


      artifactClassLoader.dispose();

      //JUL Known Levels
      assertThat(countJULKnownLevels(artifactClassLoader), is(0));
      //jmqiEnv of traceController should be null
      assertThat(getJmqiEnv(artifactClassLoader), is(nullValue()));
      //defaultMQCxManager of MQEnvironment should be null
      assertThat(getDefaultMQCxManager(artifactClassLoader), is(nullValue()));
      //myInstance field of JmsTls should be null
      assertThat(getMyInstanceFromJmsTls(artifactClassLoader), is(nullValue()));
      //TraceController should be null
      assertThat(getTraceController(artifactClassLoader), is(nullValue()));
      //no thread locals of current classLoader
      assertThat(countThreadLocals(artifactClassLoader), is(0));

    }
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
    Class jmsTlsClass = loadClass(IBM_MQ_JMS_TLS_CLASS, artifactClassLoader);
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
    Field levelObjectField = getField(knownLevelClass, "levelObject", false);
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

}
