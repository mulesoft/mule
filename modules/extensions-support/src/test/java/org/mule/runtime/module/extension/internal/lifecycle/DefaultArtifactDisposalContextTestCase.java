/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.util.CompositeClassLoader.from;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.getAllStackTraces;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;

@Feature(JAVA_SDK)
@Story(ARTIFACT_LIFECYCLE_LISTENER)
public class DefaultArtifactDisposalContextTestCase extends AbstractMuleTestCase {

  private final ArtifactClassLoader artifactClassLoader = new TestArtifactClassLoader("artifactId");
  private final ArtifactClassLoader extensionClassLoader = new TestArtifactClassLoader("extensionId");
  private final ArtifactClassLoader unrelatedArtifactClassLoader = new TestArtifactClassLoader("someOtherArtifactId");
  private final ArtifactDisposalContext artifactDisposalContext =
      new DefaultArtifactDisposalContext(artifactClassLoader, extensionClassLoader);

  private final List<AwaitingThread> awaitingThreads = new ArrayList<>();

  @After
  public void tearDown() throws InterruptedException {
    // Shuts down any thread created by the test
    for (AwaitingThread awaitingThread : awaitingThreads) {
      awaitingThread.stopGracefully();
      awaitingThread.join();
    }

    // This is for proper cleanup only, not functionally needed for any of the tests
    artifactClassLoader.dispose();
    extensionClassLoader.dispose();
    unrelatedArtifactClassLoader.dispose();
  }

  @Test
  public void directClassLoaders() {
    assertThat(artifactDisposalContext.getArtifactClassLoader(), is(sameInstance(artifactClassLoader)));
    assertThat(artifactDisposalContext.getExtensionClassLoader(), is(sameInstance(extensionClassLoader)));
    assertArtifactOwnedClassLoader(artifactClassLoader.getClassLoader());
    assertExtensionOwnedClassLoader(extensionClassLoader.getClassLoader());
  }

  @Test
  public void childClassLoaders() throws IOException {
    assertArtifactChildClassLoaders(artifactClassLoader);
    assertExtensionChildClassLoaders(extensionClassLoader);
  }

  @Test
  public void sameArtifactIdClassLoaders() throws IOException {
    // Non-child ArtifactClassLoader with the ID of the Artifact
    try (TestArtifactClassLoader childClassLoader =
        new TestArtifactClassLoader(artifactClassLoader.getArtifactId(), this.getClass().getClassLoader())) {
      assertNotOwnedClassLoader(childClassLoader.getClassLoader());
      assertChildClassLoaders((ArtifactClassLoader) childClassLoader, this::assertNotOwnedClassLoader);
    }
    // Non-child ArtifactClassLoader with the ID of the Extension
    try (TestArtifactClassLoader childClassLoader =
        new TestArtifactClassLoader(extensionClassLoader.getArtifactId(), this.getClass().getClassLoader())) {
      assertNotOwnedClassLoader(childClassLoader.getClassLoader());
      assertChildClassLoaders((ArtifactClassLoader) childClassLoader, this::assertNotOwnedClassLoader);
    }
  }

  @Test
  public void compositeClassLoaders() throws IOException {
    CompositeClassLoader compositeClassLoader;
    compositeClassLoader = from(this.getClass().getClassLoader(), artifactClassLoader.getClassLoader());
    assertArtifactOwnedClassLoader(compositeClassLoader);
    assertArtifactChildClassLoaders(compositeClassLoader);

    compositeClassLoader = from(this.getClass().getClassLoader(), extensionClassLoader.getClassLoader());
    assertExtensionOwnedClassLoader(compositeClassLoader);
    assertExtensionChildClassLoaders(compositeClassLoader);
  }

  @Test
  public void unrelatedClassLoaders() {
    assertThat(artifactDisposalContext.isArtifactOwnedClassLoader(unrelatedArtifactClassLoader.getClassLoader()), is(false));
    assertThat(artifactDisposalContext.isExtensionOwnedClassLoader(unrelatedArtifactClassLoader.getClassLoader()), is(false));
    assertThat(artifactDisposalContext.isArtifactOwnedClassLoader(this.getClass().getClassLoader()), is(false));
    assertThat(artifactDisposalContext.isExtensionOwnedClassLoader(this.getClass().getClassLoader()), is(false));
    CompositeClassLoader compositeClassLoader = from(this.getClass().getClassLoader());
    assertThat(artifactDisposalContext.isArtifactOwnedClassLoader(compositeClassLoader), is(false));
    assertThat(artifactDisposalContext.isExtensionOwnedClassLoader(compositeClassLoader), is(false));
  }

  @Test
  public void whenNoOwnedThreadsThenReturnsEmptyStream() {
    // Control test to verify that there are threads running in the current group
    assertThat(activeCount(), is(not(0)));

    assertThat("Expected no threads owned by the artifact",
               artifactDisposalContext.getArtifactOwnedThreads().count(), is(0L));
    assertThat("Expected no threads owned by the extension",
               artifactDisposalContext.getExtensionOwnedThreads().count(), is(0L));
  }

  @Test
  public void whenOwnedThreadsByDifferentArtifactThenReturnsEmptyStream() {
    startThreadWithClassLoader(unrelatedArtifactClassLoader);
    // Control test to verify that there are threads running in the current group
    assertThat(activeCount(), is(not(0)));

    assertThat("Expected no threads owned by the artifact",
               artifactDisposalContext.getArtifactOwnedThreads().count(), is(0L));
    assertThat("Expected no threads owned by the extension",
               artifactDisposalContext.getExtensionOwnedThreads().count(), is(0L));
  }

  @Test
  public void whenArtifactHasActiveThreadThenItIsReturned() {
    Thread thread = startThreadWithClassLoader(artifactClassLoader);
    assertArtifactOwnedThreads(thread);
    assertThat("Expected no threads owned by the extension",
               artifactDisposalContext.getExtensionOwnedThreads().count(), is(0L));
  }

  @Test
  public void whenExtensionHasActiveThreadThenItIsReturned() {
    Thread thread = startThreadWithClassLoader(extensionClassLoader);
    assertExtensionOwnedThreads(thread);
    assertThat("Expected no threads owned by the artifact",
               artifactDisposalContext.getArtifactOwnedThreads().count(), is(0L));
  }

  @Test
  public void whenArtifactAndExtensionHaveActiveThreadsThenTheyAreReturned() {
    Thread artifactThread = startThreadWithClassLoader(artifactClassLoader);
    Thread extensionThread = startThreadWithClassLoader(extensionClassLoader);
    assertArtifactOwnedThreads(artifactThread);
    assertExtensionOwnedThreads(extensionThread);
  }

  @Test
  public void whenOwnedThreadsAreInChildThreadGroupThenTheyAreReturned() {
    ThreadGroup threadGroup = new ThreadGroup("Test Thread Group");
    Thread artifactThread = startThreadWithClassLoader(artifactClassLoader, threadGroup);
    Thread extensionThread = startThreadWithClassLoader(extensionClassLoader, threadGroup);
    assertArtifactOwnedThreads(artifactThread);
    assertExtensionOwnedThreads(extensionThread);
  }

  @Test
  public void whenOwnedThreadsAreInSiblingThreadGroupThenTheyAreReturned() {
    ThreadGroup threadGroup = new ThreadGroup(currentThread().getThreadGroup().getParent(), "Test Thread Group");
    Thread artifactThread = startThreadWithClassLoader(artifactClassLoader, threadGroup);
    Thread extensionThread = startThreadWithClassLoader(extensionClassLoader, threadGroup);
    assertArtifactOwnedThreads(artifactThread);
    assertExtensionOwnedThreads(extensionThread);
  }

  private void assertArtifactChildClassLoaders(ArtifactClassLoader someArtifactClassLoader) throws IOException {
    assertChildClassLoaders(someArtifactClassLoader, this::assertArtifactOwnedClassLoader);
  }

  private void assertExtensionChildClassLoaders(ArtifactClassLoader someArtifactClassLoader) throws IOException {
    assertChildClassLoaders(someArtifactClassLoader, this::assertExtensionOwnedClassLoader);
  }

  private void assertArtifactChildClassLoaders(ClassLoader someClassLoader) throws IOException {
    assertChildClassLoaders(someClassLoader, this::assertArtifactOwnedClassLoader);
  }

  private void assertExtensionChildClassLoaders(ClassLoader someClassLoader) throws IOException {
    assertChildClassLoaders(someClassLoader, this::assertExtensionOwnedClassLoader);
  }

  private void assertChildClassLoaders(ArtifactClassLoader someArtifactClassLoader, Consumer<ClassLoader> classLoaderAsserter)
      throws IOException {
    // Child ArtifactClassLoader with same ID
    try (TestArtifactClassLoader childClassLoader =
        new TestArtifactClassLoader(someArtifactClassLoader.getArtifactId(), someArtifactClassLoader.getClassLoader())) {
      classLoaderAsserter.accept(childClassLoader);
    }
    assertChildClassLoaders(someArtifactClassLoader.getClassLoader(), classLoaderAsserter);
  }

  private void assertChildClassLoaders(ClassLoader someClassLoader, Consumer<ClassLoader> classLoaderAsserter)
      throws IOException {
    // Child non-artifact ClassLoader
    try (URLClassLoader childClassLoader = new URLClassLoader(new URL[] {}, someClassLoader)) {
      classLoaderAsserter.accept(childClassLoader);
    }
    // Child ArtifactClassLoader with different ID
    try (TestArtifactClassLoader childClassLoader =
        new TestArtifactClassLoader("Child Artifact", someClassLoader)) {
      classLoaderAsserter.accept(childClassLoader);
    }
  }

  private void assertArtifactOwnedClassLoader(ClassLoader classLoader) {
    assertThat(artifactDisposalContext.isArtifactOwnedClassLoader(classLoader), is(true));
    assertThat(artifactDisposalContext.isExtensionOwnedClassLoader(classLoader), is(false));
  }

  private void assertExtensionOwnedClassLoader(ClassLoader classLoader) {
    assertThat(artifactDisposalContext.isArtifactOwnedClassLoader(classLoader), is(false));
    assertThat(artifactDisposalContext.isExtensionOwnedClassLoader(classLoader), is(true));
  }

  private void assertNotOwnedClassLoader(ClassLoader classLoader) {
    assertThat(artifactDisposalContext.isArtifactOwnedClassLoader(classLoader), is(false));
    assertThat(artifactDisposalContext.isExtensionOwnedClassLoader(classLoader), is(false));
  }

  private Thread startThreadWithClassLoader(ArtifactClassLoader artifactClassLoader) {
    Thread thread = withContextClassLoader(artifactClassLoader.getClassLoader(), () -> new AwaitingThread());
    thread.start();
    return thread;
  }

  private Thread startThreadWithClassLoader(ArtifactClassLoader artifactClassLoader, ThreadGroup threadGroup) {
    Thread thread = withContextClassLoader(artifactClassLoader.getClassLoader(), () -> new AwaitingThread(threadGroup));
    thread.start();
    return thread;
  }

  private void assertArtifactOwnedThreads(Thread... threads) {
    List<Thread> artifactOwnedThreads = artifactDisposalContext.getArtifactOwnedThreads().collect(toList());
    assertThat(artifactOwnedThreads, containsInAnyOrder(threads));
    for (Thread thread : threads) {
      assertThat(artifactDisposalContext.isArtifactOwnedThread(thread), is(true));
      assertThat(artifactDisposalContext.isExtensionOwnedThread(thread), is(false));
    }

    // Also checks that the result is the same as the one from the getAllStackTraces method
    assertThat(artifactOwnedThreads, containsInAnyOrder(getArtifactOwnedThreadsFromStackTraces()));
  }

  private void assertExtensionOwnedThreads(Thread... threads) {
    List<Thread> extensionOwnedThreads = artifactDisposalContext.getExtensionOwnedThreads().collect(toList());
    assertThat(extensionOwnedThreads, containsInAnyOrder(threads));
    for (Thread thread : threads) {
      assertThat(artifactDisposalContext.isArtifactOwnedThread(thread), is(false));
      assertThat(artifactDisposalContext.isExtensionOwnedThread(thread), is(true));
    }

    // Also checks that the result is the same as the one from the getAllStackTraces method
    assertThat(extensionOwnedThreads, containsInAnyOrder(getExtensionOwnedThreadsFromStackTraces()));
  }

  private Thread[] getArtifactOwnedThreadsFromStackTraces() {
    return getAllStackTraces().keySet().stream().filter(artifactDisposalContext::isArtifactOwnedThread).toArray(Thread[]::new);
  }

  private Thread[] getExtensionOwnedThreadsFromStackTraces() {
    return getAllStackTraces().keySet().stream().filter(artifactDisposalContext::isExtensionOwnedThread).toArray(Thread[]::new);
  }

  private static class TestArtifactClassLoader extends MuleArtifactClassLoader {

    public TestArtifactClassLoader(String artifactId) {
      this(artifactId, DefaultArtifactDisposalContextTestCase.class.getClassLoader());
    }

    public TestArtifactClassLoader(String artifactId, ClassLoader parent) {
      super(artifactId, new ArtifactDescriptor(artifactId), new URL[] {}, parent, new ParentFirstLookupPolicy());
    }
  }

  private static class ParentFirstLookupPolicy implements ClassLoaderLookupPolicy {

    @Override
    public LookupStrategy getClassLookupStrategy(String className) {
      return PARENT_FIRST;
    }

    @Override
    public LookupStrategy getPackageLookupStrategy(String packageName) {
      return PARENT_FIRST;
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
  }

  private class AwaitingThread extends Thread {

    private boolean stopRequested = false;

    public AwaitingThread() {
      this(null);
    }

    public AwaitingThread(ThreadGroup threadGroup) {
      // Gives it a custom name just for easier debugging
      super(threadGroup, "OwnedThread");
      // Tracks every thread generated by the tests to ensure proper cleanup
      awaitingThreads.add(this);
    }

    @Override
    public void run() {
      synchronized (this) {
        while (!stopRequested) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            // Does nothing
          }
        }
      }
    }

    public synchronized void stopGracefully() {
      stopRequested = true;
      this.notify();
    }
  }
}
