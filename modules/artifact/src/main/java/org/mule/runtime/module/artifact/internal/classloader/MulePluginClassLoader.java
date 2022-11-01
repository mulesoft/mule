/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import static java.lang.reflect.Modifier.isAbstract;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.module.artifact.classloader.ActiveMQResourceReleaser;
import org.mule.module.artifact.classloader.ClassLoaderResourceReleaser;
import org.mule.module.artifact.classloader.IBMMQResourceReleaser;
import org.mule.module.artifact.classloader.MvelClassLoaderReleaser;
import org.mule.module.artifact.classloader.ScalaClassValueReleaser;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.InputStream;
import java.net.URL;
import java.sql.Driver;

import org.slf4j.Logger;

public class MulePluginClassLoader extends MuleArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  private static final Logger LOGGER = getLogger(MuleArtifactClassLoader.class);

  private static final String DB_RESOURCE_RELEASER_CLASS_LOCATION =
      "/org/mule/module/artifact/classloader/JdbcResourceReleaser.class";

  private String dbResourceReleaserClassLocation = DB_RESOURCE_RELEASER_CLASS_LOCATION;
  private final ResourceReleaser classLoaderReferenceReleaser;
  private volatile boolean shouldReleaseJdbcReferences = false;
  private volatile boolean shouldReleaseIbmMQResources = false;
  private volatile boolean shouldReleaseActiveMQReferences = false;
  private ResourceReleaser jdbcResourceReleaserInstance;
  private final ResourceReleaser scalaClassValueReleaserInstance;
  private final ResourceReleaser mvelClassLoaderReleaserInstance;

  /**
   * Constructs a new {@link MuleArtifactClassLoader} for the given URLs
   *
   * @param artifactId         artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader. Non null.
   * @param urls               the URLs from which to load classes and resources
   * @param parent             the parent class loader for delegation
   * @param lookupPolicy       policy used to guide the lookup process. Non null
   */
  public MulePluginClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                               ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);

    this.classLoaderReferenceReleaser = new ClassLoaderResourceReleaser(this);
    this.scalaClassValueReleaserInstance = new ScalaClassValueReleaser();
    this.mvelClassLoaderReleaserInstance = new MvelClassLoaderReleaser(this);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> clazz = super.loadClass(name, resolve);
    if (!shouldReleaseJdbcReferences && Driver.class.isAssignableFrom(clazz) &&
        !(clazz.equals(Driver.class) || clazz.isInterface() || isAbstract(clazz.getModifiers()))) {
      shouldReleaseJdbcReferences = true;
    }
    if (!shouldReleaseIbmMQResources && name.startsWith("com.ibm.mq")) {
      shouldReleaseIbmMQResources = true;
    }

    if (!shouldReleaseActiveMQReferences && name.startsWith("org.apache.activemq")) {
      shouldReleaseActiveMQReferences = true;
    }
    return clazz;
  }

  @Override
  protected void doDispose() {
    try {
      clearReferences();
    } catch (Exception e) {
      reportPossibleLeak(e, getArtifactId());
    }

    try {
      if (shouldReleaseJdbcReferences) {
        createResourceReleaserInstance().release();
      }
    } catch (Exception e) {
      reportPossibleLeak(e, getArtifactId());
    }

    if (shouldReleaseIbmMQResources) {
      new IBMMQResourceReleaser(this).release();
    }

    if (shouldReleaseActiveMQReferences) {
      new ActiveMQResourceReleaser(this).release();
    }
  }

  private void clearReferences() {
    classLoaderReferenceReleaser.release();
    scalaClassValueReleaserInstance.release();
    mvelClassLoaderReleaserInstance.release();
  }

  /**
   * Creates a {@link ResourceReleaser} using this classloader, only used outside in unit tests.
   */
  protected ResourceReleaser createResourceReleaserInstance() {
    if (jdbcResourceReleaserInstance == null) {
      jdbcResourceReleaserInstance = createInstance(dbResourceReleaserClassLocation);
    }
    return jdbcResourceReleaserInstance;
  }

  public void setResourceReleaserClassLocation(String resourceReleaserClassLocation) {
    this.dbResourceReleaserClassLocation = resourceReleaserClassLocation;
  }

  private <T> T createInstance(String classLocation) {
    try {
      Class clazz = createClass(classLocation);
      return (T) clazz.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Can not create instance from resource: " + classLocation, e);
    }
  }

  private Class createClass(String classLocation) {
    InputStream classStream = null;
    try {
      classStream = this.getClass().getResourceAsStream(classLocation);
      byte[] classBytes = IOUtils.toByteArray(classStream);
      classStream.close();
      return this.defineClass(null, classBytes, 0, classBytes.length);
    } catch (Exception e) {
      throw new RuntimeException("Can not create class from resource: " + classLocation, e);
    } finally {
      closeQuietly(classStream);
    }
  }


}
