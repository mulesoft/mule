/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.container.api.CoreExtensionsAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.container.api.MuleCoreExtensionDependency;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mockito.Matchers;

@SmallTest
public class ReflectionMuleCoreExtensionDependencyResolverTestCase extends AbstractMuleTestCase {

  public static final String EXTENSION1 = "extension1";
  public static final String EXTENSION2 = "extension2";
  public static final String DEPENDANT_EXTENSION = "dependantCoreExtension";

  private MuleCoreExtensionDependencyDiscoverer dependencyDiscoverer = mock(MuleCoreExtensionDependencyDiscoverer.class);
  private ReflectionMuleCoreExtensionDependencyResolver dependencyResolver =
      new ReflectionMuleCoreExtensionDependencyResolver(dependencyDiscoverer);

  @Test
  public void resolvesEmptyDependencies() throws Exception {
    List<MuleCoreExtension> coreExtensions = Collections.EMPTY_LIST;
    dependencyResolver.resolveDependencies(coreExtensions);
    verify(dependencyDiscoverer, times(0)).findDependencies(Matchers.<MuleCoreExtension>anyObject());
  }

  @Test
  public void resolvesIndependentExtensions() throws Exception {
    MuleCoreExtension extension1 = mockCoreExtension(EXTENSION1);
    MuleCoreExtension extension2 = mockCoreExtension(EXTENSION2);

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(extension2);
    coreExtensions.add(extension1);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);
    verify(dependencyDiscoverer, times(2)).findDependencies(Matchers.<MuleCoreExtension>anyObject());

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), sameInstance(extension1));
    assertThat(resolvedCoreExtensions.get(1), sameInstance(extension2));
  }

  @Test
  public void resolvesDependentExtensionWhenDependencyIsCreatedFirst() throws Exception {
    TestCoreExtension testCoreExtension = new TestCoreExtension();
    TestCoreExtensionDependency dependantCoreExtension = mock(TestCoreExtensionDependency.class);
    when(dependantCoreExtension.getName()).thenReturn(DEPENDANT_EXTENSION);

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(testCoreExtension);
    coreExtensions.add(dependantCoreExtension);

    final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<>();
    final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency =
        new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class
            .getMethod("setTestCoreExtension", TestCoreExtension.class));
    dependencies.add(linkedMuleCoreExtensionDependency);

    when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

    verify(dependantCoreExtension).setTestCoreExtension(testCoreExtension);

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), is(instanceOf(TestCoreExtension.class)));
    assertThat(resolvedCoreExtensions.get(1), is(instanceOf(TestCoreExtensionDependency.class)));
  }

  @Test
  public void resolvesDependentExtensionWhenDependantIsCreatedFirst() throws Exception {
    TestCoreExtension testCoreExtension = new TestCoreExtension();
    TestCoreExtensionDependency dependantCoreExtension = mock(TestCoreExtensionDependency.class);
    when(dependantCoreExtension.getName()).thenReturn(DEPENDANT_EXTENSION);

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(dependantCoreExtension);
    coreExtensions.add(testCoreExtension);

    final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<>();
    final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency =
        new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class
            .getMethod("setTestCoreExtension", TestCoreExtension.class));
    dependencies.add(linkedMuleCoreExtensionDependency);

    when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

    verify(dependantCoreExtension).setTestCoreExtension(testCoreExtension);

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), is(instanceOf(TestCoreExtension.class)));
    assertThat(resolvedCoreExtensions.get(1), is(instanceOf(TestCoreExtensionDependency.class)));
  }

  @Test
  public void resolvesCoreExtensionAwareAndStandardExtensions() throws Exception {
    MuleCoreExtension coreExtensionsAwareExtension = mockCoreExtension(EXTENSION1);
    MuleCoreExtension testCoreExtension = new TestCoreExtension();

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(testCoreExtension);
    coreExtensions.add(coreExtensionsAwareExtension);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), sameInstance(testCoreExtension));
    assertThat(resolvedCoreExtensions.get(1), sameInstance(coreExtensionsAwareExtension));
  }

  @Test
  public void resolvesStandardAndCoreExtensionAwareExtensions() throws Exception {
    MuleCoreExtension coreExtensionsAwareExtension = mockCoreExtension("coreExtensionsAwareExtension");
    MuleCoreExtension testCoreExtension = new TestCoreExtension();

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(coreExtensionsAwareExtension);
    coreExtensions.add(testCoreExtension);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), sameInstance(testCoreExtension));
    assertThat(resolvedCoreExtensions.get(1), sameInstance(coreExtensionsAwareExtension));
  }

  @Test
  public void resolvesOrderedCoreExtensionAwareExtensions() throws Exception {
    MuleCoreExtension coreExtensionsAwareExtension1 = mockCoreExtension(EXTENSION1);
    MuleCoreExtension coreExtensionsAwareExtension2 = mockCoreExtension(EXTENSION2);

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(coreExtensionsAwareExtension1);
    coreExtensions.add(coreExtensionsAwareExtension2);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), sameInstance(coreExtensionsAwareExtension1));
    assertThat(resolvedCoreExtensions.get(1), sameInstance(coreExtensionsAwareExtension2));
  }

  @Test
  public void resolvesDisorderedCoreExtensionAwareExtensions() throws Exception {
    MuleCoreExtension coreExtensionsAwareExtension1 = mockCoreExtension(EXTENSION1);
    MuleCoreExtension coreExtensionsAwareExtension2 = mockCoreExtension(EXTENSION2);

    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    coreExtensions.add(coreExtensionsAwareExtension2);
    coreExtensions.add(coreExtensionsAwareExtension1);

    final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

    assertThat(resolvedCoreExtensions.size(), equalTo(2));
    assertThat(resolvedCoreExtensions.get(0), sameInstance(coreExtensionsAwareExtension1));
    assertThat(resolvedCoreExtensions.get(1), sameInstance(coreExtensionsAwareExtension2));
  }

  @Test(expected = UnresolveableDependencyException.class)
  public void throwsExceptionOnUnresolvedDependency() throws Exception {
    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    TestCoreExtensionDependency dependantCoreExtension = mock(TestCoreExtensionDependency.class);
    coreExtensions.add(dependantCoreExtension);

    final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<>();
    final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency =
        new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class
            .getMethod("setTestCoreExtension", TestCoreExtension.class));
    dependencies.add(linkedMuleCoreExtensionDependency);
    when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

    dependencyResolver.resolveDependencies(coreExtensions);
  }

  @Test(expected = IllegalDependencyInjectionException.class)
  public void throwsExceptionOnIllegalDependency() throws Exception {
    List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    TestIllegalCoreExtensionDependency dependantCoreExtension = mock(TestIllegalCoreExtensionDependency.class);
    coreExtensions.add(dependantCoreExtension);

    final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<>();
    final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency =
        new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class
            .getMethod("setTestCoreExtension", TestCoreExtension.class));
    dependencies.add(linkedMuleCoreExtensionDependency);
    when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

    dependencyResolver.resolveDependencies(coreExtensions);
  }

  private MuleCoreExtension mockCoreExtension(String extension11) {
    MuleCoreExtension extension1 = mock(CoreExtensionsAwareExtension.class);
    when(extension1.getName()).thenReturn(extension11);

    return extension1;
  }

  public static class AbstractTestCoreExtension implements MuleCoreExtension {

    @Override
    public void dispose() {}

    @Override
    public void initialise() throws InitialisationException {}

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void start() throws MuleException {}

    @Override
    public void stop() throws MuleException {}

    @Override
    public void setContainerClassLoader(ArtifactClassLoader containerClassLoader) {

    }
  }

  public static class TestCoreExtension extends AbstractTestCoreExtension {

    @Override
    public String getName() {
      return "testCoreExtension";
    }
  }

  public interface TestCoreExtensionDependency extends MuleCoreExtension {

    @MuleCoreExtensionDependency
    void setTestCoreExtension(TestCoreExtension coreExtension);
  }

  public interface TestIllegalCoreExtensionDependency extends MuleCoreExtension, CoreExtensionsAware {

    @MuleCoreExtensionDependency
    void setTestCoreExtension(TestCoreExtension coreExtension);
  }

  public interface CoreExtensionsAwareExtension extends MuleCoreExtension, CoreExtensionsAware {

  }
}
