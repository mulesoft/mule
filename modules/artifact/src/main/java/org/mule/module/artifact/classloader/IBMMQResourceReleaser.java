/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.beans.Introspector.flushCaches;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


public class IBMMQResourceReleaser implements ResourceReleaser {

  private static final Logger LOGGER = LoggerFactory.getLogger(IBMMQResourceReleaser.class);
  private static final String JMSCC_THREAD_POOL_MASTER_NAME = "JMSCCThreadPoolMaster";

  private final static String THREADLOCALS_FIELD = "threadLocals";
  private final static String INHERITABLE_THREADLOCALS_FIELD = "inheritableThreadLocals";
  private final static String THREADLOCAL_MAP_TABLE_CLASS = "java.lang.ThreadLocal$ThreadLocalMap";
  private final static String JUL_KNOWN_LEVEL_CLASS = "java.util.logging.Level$KnownLevel";
  private final static String IBM_MQ_MBEAN_DOMAIN = "IBM MQ";
  private final static String IBM_MQ_WORK_QUEUE_MANAGER_CLASS = "com.ibm.msg.client.commonservices.workqueue.WorkQueueManager";
  private final static String IBM_MQ_ENVIRONMENT_CLASS = "com.ibm.mq.MQEnvironment";
  private final static String IBM_MQ_COMMON_SERVICES_CLASS = "com.ibm.mq.internal.MQCommonServices";
  private final ClassLoader driverClassLoader;


  @Override
  public void release() {
    LOGGER.debug("Releasing IBM MQ resources");
    //TODO add property to avoid releasing this resources
    removeMBeans();
    cleanJULKnownLevels();
    cleanMQCommonServices();
    cleanMQEnvironment();
    cleanJmsTls();
    cleanTraceController();
    removeThreadLocals();
  }

  /**
   * Creates a new Instance of IBMMQResourceReleaser
   */
  public IBMMQResourceReleaser(ClassLoader classLoader) {
    this.driverClassLoader = classLoader;
  }

  /**
   * Disposes all the IBM MQ related threads.
   */
  public void disposeMQThreads() {
    for (Thread thread : getAllThreads()) {
      if (thread.getName().equals(JMSCC_THREAD_POOL_MASTER_NAME)) {
        killThreadPoolMaster(thread);
      }
    }
    try {
      Class<?> wqmClass = Class.forName(IBM_MQ_WORK_QUEUE_MANAGER_CLASS, false, driverClassLoader);
      Method m = wqmClass.getDeclaredMethod("close");
      m.invoke(null);
    } catch (Throwable e) {
      LOGGER.warn("An error occurred trying to close the WorkQueueManager", e);
    }
  }

  /**
   * Removes the two known registered MBeans of the IBM MQ Driver.
   *  * TraceControl
   *  * PropertyStoreControl
   */
  public void removeMBeans() {

    /*
    The IBM MQ Driver registers two MBeans for management.
    When disposing the application / domain, this beans keep references
    to classes loaded by this ClassLoader. When the application is removed,
    the ClassLoader is leaked due this references.
     */

    // IBM MQ // IBM WebSphere MQ
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Removing registered MBeans of the IBM MQ Driver (if present)");
    }

    MBeanServer mBeanServer = getPlatformMBeanServer();
    Set<ObjectInstance> instances = mBeanServer.queryMBeans(null, null);
    if (LOGGER.isDebugEnabled()) {
      instances.stream()
          .forEach(x -> LOGGER.debug("MBean Found: Class Name: {} // Object Name: {}", x.getClassName(), x.getObjectName()));
    }

    //Object Name::type=CommonServices,name=TraceControl
    final Hashtable<String, String> keys = new Hashtable<>();
    keys.put("type", "CommonServices");
    keys.put("name", "TraceControl");
    try {
      if (driverClassLoader == mBeanServer.getClassLoaderFor(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys))) {
        mBeanServer.unregisterMBean(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys));
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Unregistered IBM MQ TraceControl MBean");
      }
    } catch (javax.management.InstanceNotFoundException | MalformedObjectNameException | MBeanRegistrationException e) {
      LOGGER.warn(e.getMessage(), e);
    }

    //IBM WebSphere MQ:type=CommonServices,name=PropertyStoreControl
    keys.put("type", "CommonServices");
    keys.put("name", "PropertyStoreControl");
    try {
      if (driverClassLoader == mBeanServer.getClassLoaderFor(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys))) {
        mBeanServer.unregisterMBean(new ObjectName(IBM_MQ_MBEAN_DOMAIN, keys));
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Unregistered IBM MQ PropertyStoreControl MBean");
      }
    } catch (javax.management.InstanceNotFoundException | MalformedObjectNameException | MBeanRegistrationException e) {
      LOGGER.warn("Caught exception removing known IBM MQ Mbeans: {}", e.getMessage(), e);
    }
    flushCaches();
  }

  /**
   * Clears the references for the Java Util Logging.
   */
  public void cleanJULKnownLevels() {
    /*
        The IBM MQ Driver registers custom Java Util Logging (JUL) Levels.
        the JUL Classes are loaded by system Classloader. So there are references left
        from outside the application context. So, it retains instances and leaks the application
        ClassLoader.
     */
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Cleaning Java Util Logging references");
    }
    try {
      Class<?> knownLevelClass =
          Class.forName(JUL_KNOWN_LEVEL_CLASS, false, driverClassLoader);
      Field levelObjectField = getField(knownLevelClass, "levelObject");

      synchronized (knownLevelClass) {
        final Map<?, List> nameToLevels = (Map<?, List>) getStaticFieldValue(knownLevelClass, "nameToLevels");
        final Map<?, List> intToLevels = (Map<?, List>) getStaticFieldValue(knownLevelClass, "intToLevels");
        if (nameToLevels != null) {
          final Set removed = processJULKnownLevels(levelObjectField, nameToLevels);
          if (intToLevels != null) {
            for (List knownLevels : intToLevels.values()) {
              knownLevels.removeAll(removed);
            }
          }
        } else if (intToLevels != null) {
          processJULKnownLevels(levelObjectField, intToLevels);
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Caught error when removing JUL References: {}", e.getMessage(), e);
    }
  }

  /**
   * Processes and removes the Java Util Logging KnownLevels references
   * @param levelObjectField Field to access internal property.
   * @param levelsMaps Map wich contains references
   * @return Removed KnownLevels
   * @throws Exception
   */
  private Set processJULKnownLevels(
                                    Field levelObjectField, Map<?, List> levelsMaps)
      throws Exception {
    Set output = new HashSet<>();
    for (List knownLevels : levelsMaps.values()) {
      for (Iterator iter = knownLevels.listIterator(); iter.hasNext();) {
        Object knownLevel = iter.next();
        Level levelObject = (Level) levelObjectField.get(knownLevel);
        if (driverClassLoader == levelObject.getClass().getClassLoader()) {
          iter.remove();
          output.add(knownLevel);
        }
      }
    }
    return output;
  }

  /**
   * Kills the ThreadPool master thread for IBM classes.
   * @param thread
   */
  private void killThreadPoolMaster(Thread thread) {
    Class<? extends Thread> threadClass = thread.getClass();
    boolean shouldCleanThread = isThreadFromCurrentClassLoader(threadClass);
    if (shouldCleanThread) {
      try {
        Method closeMethod = threadClass.getDeclaredMethod("close");
        closeMethod.setAccessible(true);
        closeMethod.invoke(thread);
        thread.interrupt();
      } catch (Throwable e) {
        LOGGER.warn("Caught exception when closing the '" + JMSCC_THREAD_POOL_MASTER_NAME + "' Thread: {}", e.getMessage(), e);
      }
    }
  }


  /**
   * Clears the JmsTls static final references.
   */
  private void cleanJmsTls() {
    /*
    The JmsTls class keep several references in a private static final field
    This references avoid the proper ClassLoader disposal.
     */
    try {
      //JmsTls
      Class myClass = Class.forName("com.ibm.msg.client.jms.internal.JmsTls", false, driverClassLoader);
      clearPrivateStaticField(myClass, "myInstance", true);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception when clearing JmsTls: {}", ex.getMessage(), ex);
    }
  }

  /**
   * Clears the TraceController static final reference.
   */
  private void cleanTraceController() {
    /*
    The TraceController classes keep several references in private static final field
    This references avoid the proper ClassLoader disposal.
     */
    try {
      //TraceController
      Class myClass = Class.forName("com.ibm.msg.client.commonservices.trace.Trace", false, driverClassLoader);
      clearPrivateStaticField(myClass, "traceController", false);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception when clearing TraceController: {}", ex.getMessage(), ex);
    }
  }


  /**
   * Removes thread local variables registered in non application threads.
   */
  public void removeThreadLocals() {

    /*
    While analyzing the HeapDumps looking for the ClassLoader Leak causes,
    there were ThreadLocal references to driver classes in threads that do not belong to
    the application being disposed. This references leads to a ClassLoader leak.
     */

    Field threadLocalsField = null;
    Field inheritableThreadLocalsField = null;
    Field threadLocalMapTableField = null;

    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Removing ThreadLocals");
      }
      threadLocalsField = getField(Thread.class, THREADLOCALS_FIELD);
      inheritableThreadLocalsField = getField(Thread.class, INHERITABLE_THREADLOCALS_FIELD);
      Class<?> threadLocalMapTableClass = Class.forName(THREADLOCAL_MAP_TABLE_CLASS);
      threadLocalMapTableField = getField(threadLocalMapTableClass, "table");
    } catch (NoSuchFieldException | ClassNotFoundException ex) {
      LOGGER.warn("Caught Exception  while getting required fields: {}", ex.getMessage(), ex);
    }


    for (Thread thread : getAllThreads()) {
      LOGGER.debug("Processing Thread: {} / {}", thread.getThreadGroup().getName(), thread.getName());

      if (nonNull(threadLocalsField)) {
        try {
          processThreadLocalMap(threadLocalMapTableField, threadLocalsField.get(thread));
        } catch (IllegalAccessException ex) {
          LOGGER.warn("Caught exception getting ThreadLocals Field of {} thread: {}", thread.getName(), ex.getMessage(), ex);
        }
      }
      if (nonNull(inheritableThreadLocalsField)) {
        try {
          processThreadLocalMap(threadLocalMapTableField, inheritableThreadLocalsField.get(thread));
        } catch (IllegalAccessException ex) {
          LOGGER.warn("Caught exception getting InheritableThreadLocals Field of {} thread: {}", thread.getName(),
                      ex.getMessage(), ex);
        }
      }
    }

  }


  /**
   * Removes the threadLocal variables related to classes Loaded by this classloader.
   * @param threadLocalMap Map containing ThreadLocal values
   */
  private void processThreadLocalMap(Field threadLocalMapTableField, Object threadLocalMap) {

    if (nonNull(threadLocalMap) && nonNull(threadLocalMapTableField)) {

      Object[] threadLocalMapTable = {};

      try {
        threadLocalMapTable = (Object[]) threadLocalMapTableField.get(threadLocalMap);
      } catch (IllegalAccessException e) {
        LOGGER.error("Could not get threadLocalMapTablefield: {}", e.getMessage(), e);
      }

      for (Object entry : threadLocalMapTable) {

        if (entry != null) {
          Reference<?> reference = (Reference<?>) entry;
          ThreadLocal<?> threadLocal = (ThreadLocal<?>) reference.get();
          if (nonNull(threadLocal)) {
            try {

              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ClassLoader: {}", this.getClass().getClassLoader().getClass().getName());
              }
              Object x = threadLocal.get();
              if (nonNull(x)) {
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("ThreadLocal ClassLoader: {}", x.getClass().getClassLoader());
                  LOGGER.debug("ThreadLocal Class: {}", x.getClass().getCanonicalName());
                }
                if (driverClassLoader == x.getClass().getClassLoader()) {
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Removing instance of {}", x.getClass().getCanonicalName());
                  }
                  threadLocal.remove();
                  threadLocal.set(null);
                  reference.clear();
                  Field threadLocalMapEntryValueField = getField(entry.getClass(), "value");
                  threadLocalMapEntryValueField.set(entry, null);
                  ((Reference<?>) entry).clear();
                }
              }

            } catch (IllegalAccessException | NoSuchFieldException e) {
              LOGGER.warn("Caught Exception while accessing threadLocalMapEntryValueField: {}", e.getMessage(), e);
            }

          }
        }
      }
    }
  }


  /**
   * Removes references held by MQCommonServices.
   */
  public void cleanMQCommonServices() {
    try {
      Class<?> mqCommonServicesClass = Class.forName(IBM_MQ_COMMON_SERVICES_CLASS, false, driverClassLoader);
      clearPrivateStaticField(mqCommonServicesClass, "jmqiEnv", true);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception cleaning MQCommonServices: {}", ex.getMessage(), ex);
    }
  }

  /**
   * Removes the static references held by the MQEnvironment Class
   */
  public void cleanMQEnvironment() {
    try {
      Class<?> mqEnvironmentClass = Class.forName(IBM_MQ_ENVIRONMENT_CLASS, false, driverClassLoader);
      clearPrivateStaticField(mqEnvironmentClass, "defaultMQCxManager", false);
    } catch (Exception ex) {
      LOGGER.warn("Caught Exception cleaning MQEnvironment: {}", ex.getMessage(), ex);
    }
  }



  /**
   * Checks if the thread belongs to this class ClassLoader
   * @param threadClass The thread to validate
   * @return true if the thread belongs to this class ClassLoader
   */
  private boolean isThreadFromCurrentClassLoader(Class<? extends Thread> threadClass) {
    ClassLoader threadParentClassLoader = threadClass.getClassLoader().getParent();
    ClassLoader connectorParentClassLoader = this.getClass().getClassLoader().getParent();
    return connectorParentClassLoader == threadParentClassLoader;
  }


  /**
   * Lists all the threads.
   * @return a List including all the threads.
   */
  private List<Thread> getAllThreads() {

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    while (threadGroup.getParent() != null) {
      threadGroup = threadGroup.getParent();
    }

    int threadCount = threadGroup.activeCount() + 100;
    Thread[] threads = new Thread[threadCount];
    int actualThreadCount = threadGroup.enumerate(threads);

    while (actualThreadCount == threadCount) {
      threadCount *= 2;
      threads = new Thread[threadCount];
      actualThreadCount = threadGroup.enumerate(threads);
    }
    return Arrays.stream(threads).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Clears a private static field.
   * @param c Target Class
   * @param fieldName Target field.
   * @param isFinal indicates if the target field is final
   * @throws Exception The field does not exists or is not accesible.
   */
  private void clearPrivateStaticField(Class<?> c, String fieldName, boolean isFinal) throws Exception {
    clearPrivateField(c, fieldName, null, isFinal);
  }

  /***
   * Clears a private field value.
   * @param c Class of the target
   * @param fieldName field to clear
   * @param instance Instance to remove the field value. Null for static fields.
   * @param isFinal indicates if the target field is final
   * @throws Exception The field does not exists or is not accesible.
   */
  private void clearPrivateField(Class<?> c, String fieldName, Object instance, boolean isFinal) throws Exception {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Clearing{}{} field of class {}", isNull(instance) ? " static" : "", isFinal ? " final" : "",
                   c.getCanonicalName());
    }
    Field field = getField(c, fieldName);
    if (isFinal) {
      Field modifiersField = getField(Field.class, "modifiers");
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
    field.set(instance, null);
  }

  /**
   * Gets the desired field of a class making it accessible.
   * @param classArg Class of the requested field
   * @param name field Name
   * @return Selected field
   * @throws NoSuchFieldException The selected field does not exists.
   */
  private Field getField(Class<?> classArg, String name) throws NoSuchFieldException {
    Field field = classArg.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  /**
   * Gets the private static field value for a given clasess
   * @param c the target class
   * @param fieldName the target field
   * @return the value for the target field of the given class.
   */
  private Object getStaticFieldValue(Class<?> c, String fieldName) {
    try {
      Field field = getField(c, fieldName);
      return field.get(null);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return null;
    }
  }

}
