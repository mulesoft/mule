/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.module.artifact.classloader.DependencyResolver.getDependencyFromMaven;
import static org.mule.runtime.module.artifact.classloader.SimpleClassLoaderLookupPolicy.CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static java.lang.Thread.currentThread;

import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.mule.module.artifact.classloader.AwsIdleConnectionReaperResourceReleaser;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;

@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
public class AwsIdleConnectionReaperResourceReleaserTestCase extends AbstractMuleTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;

  private static final String AWS_SDK_GROUP_ID = "com.amazonaws";
  private static final String AWS_SDK_ARTIFACT_ID = "aws-java-sdk-core";
  private static final String AWS_SDK_VERSION = "1.12.457";

  private static final String IDLE_CONNECTION_REAPER_CLASS_NAME = "com.amazonaws.http.IdleConnectionReaper";

  private MuleArtifactClassLoader classLoader = TestArtifactClassLoader.createFrom(this.getClass(),
                                                                                   getDependencyFromMaven(AWS_SDK_GROUP_ID,
                                                                                                          AWS_SDK_ARTIFACT_ID,
                                                                                                          AWS_SDK_VERSION));

  @Before
  public void setup() throws Exception {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), is(false));
  }

  @Test
  public void idleConnectionReaperThreadIsDisposed() throws ReflectiveOperationException {
    startIdleConnectionReaperThread();
    classLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  @Test
  public void releaserCanBeCalledMultipleTimes() throws ReflectiveOperationException {
    startIdleConnectionReaperThread();
    new AwsIdleConnectionReaperResourceReleaser(classLoader).release();
    classLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  private void startIdleConnectionReaperThread() throws ReflectiveOperationException {
    // Creates a dummy connection manager which does nothing but retain a hard reference to the artifact's ClassLoader
    HttpClientConnectionManager retainingConnectionManager = new ClassLoaderRetainingConnectionManager(classLoader);

    // Registers the connection manager with the IdleConnectionReaper, this will ensure the thread is started
    // Also, we want the thread to have the artifact's ClassLoader as its TCCL
    Class<?> idleConnectionReaperClass = classLoader.loadClass(IDLE_CONNECTION_REAPER_CLASS_NAME);
    ClassLoader currentTCCL = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(classLoader);
      idleConnectionReaperClass.getMethod("registerConnectionManager", HttpClientConnectionManager.class)
          .invoke(null, retainingConnectionManager);
    } finally {
      currentThread().setContextClassLoader(currentTCCL);
    }
  }

  private void assertClassLoaderIsEnqueued() {
    PhantomReference<ClassLoader> artifactClassLoaderRef = new PhantomReference<>(classLoader, new ReferenceQueue<>());
    classLoader = null;

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(artifactClassLoaderRef.isEnqueued(), is(true));
      return true;
    }));
  }

  private static class TestArtifactClassLoader extends MuleDeployableArtifactClassLoader {

    public static TestArtifactClassLoader createFrom(Class<?> testClass, URL... urls) {
      return new TestArtifactClassLoader(testClass.getClassLoader(), urls);
    }

    private TestArtifactClassLoader(ClassLoader parentCl, URL[] urls) {
      super("testId", new ArtifactDescriptor("test"), urls, parentCl, CHILD_FIRST_CLASSLOADER_LOOKUP_POLICY);
    }
  }

  private static class ClassLoaderRetainingConnectionManager implements HttpClientConnectionManager {

    final ClassLoader retainedClassLoader;

    private ClassLoaderRetainingConnectionManager(ClassLoader retainedClassLoader) {
      this.retainedClassLoader = retainedClassLoader;
    }

    @Override
    public ConnectionRequest requestConnection(HttpRoute httpRoute, Object o) {
      // Dummy
      return null;
    }

    @Override
    public void releaseConnection(HttpClientConnection httpClientConnection, Object o, long l, TimeUnit timeUnit) {
      // Does nothing
    }

    @Override
    public void connect(HttpClientConnection httpClientConnection, HttpRoute httpRoute, int i, HttpContext httpContext)
        throws IOException {
      // Does nothing
    }

    @Override
    public void upgrade(HttpClientConnection httpClientConnection, HttpRoute httpRoute, HttpContext httpContext)
        throws IOException {
      // Does nothing
    }

    @Override
    public void routeComplete(HttpClientConnection httpClientConnection, HttpRoute httpRoute, HttpContext httpContext)
        throws IOException {
      // Does nothing
    }

    @Override
    public void closeIdleConnections(long l, TimeUnit timeUnit) {
      // Does nothing
    }

    @Override
    public void closeExpiredConnections() {
      // Does nothing
    }

    @Override
    public void shutdown() {
      // Does nothing
    }
  }

}
