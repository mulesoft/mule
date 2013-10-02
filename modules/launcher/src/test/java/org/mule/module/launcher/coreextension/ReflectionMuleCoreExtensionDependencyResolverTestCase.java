/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.coreextension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.MuleCoreExtension;
import org.mule.MuleCoreExtensionDependency;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mockito.Matchers;

@SmallTest
    public class ReflectionMuleCoreExtensionDependencyResolverTestCase extends AbstractMuleTestCase
{

    private MuleCoreExtensionDependencyDiscoverer dependencyDiscoverer = mock(MuleCoreExtensionDependencyDiscoverer.class);
    private ReflectionMuleCoreExtensionDependencyResolver dependencyResolver = new ReflectionMuleCoreExtensionDependencyResolver(dependencyDiscoverer);

    @Test
    public void resolvesEmptyDependencies() throws Exception
    {
        List<MuleCoreExtension> coreExtensions = Collections.EMPTY_LIST;
        dependencyResolver.resolveDependencies(coreExtensions);
        verify(dependencyDiscoverer, times(0)).findDependencies(Matchers.<MuleCoreExtension>anyObject());
    }

    @Test
    public void resolvesIndependentExtension() throws Exception
    {
        List<MuleCoreExtension> coreExtensions = new LinkedList<MuleCoreExtension>();
        coreExtensions.add(new TestCoreExtension());

        final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);
        verify(dependencyDiscoverer).findDependencies(Matchers.<MuleCoreExtension>anyObject());

        assertThat(resolvedCoreExtensions.size(), equalTo(1));
        assertThat(resolvedCoreExtensions.get(0), is(TestCoreExtension.class));
    }

    @Test
    public void resolvesDependentExtensionWhenDependencyIsCreatedFirst() throws Exception
    {
        TestCoreExtension testCoreExtension = new TestCoreExtension();
        TestCoreExtensionDependency dependantCoreExtension = mock(TestCoreExtensionDependency.class);

        List<MuleCoreExtension> coreExtensions = new LinkedList<MuleCoreExtension>();
        coreExtensions.add(testCoreExtension);
        coreExtensions.add(dependantCoreExtension);

        final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<LinkedMuleCoreExtensionDependency>();
        final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency = new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class.getMethod("setTestCoreExtension", TestCoreExtension.class));
        dependencies.add(linkedMuleCoreExtensionDependency);

        when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

        final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

        verify(dependantCoreExtension).setTestCoreExtension(testCoreExtension);

        assertThat(resolvedCoreExtensions.size(), equalTo(2));
        assertThat(resolvedCoreExtensions.get(0), is(TestCoreExtension.class));
        assertThat(resolvedCoreExtensions.get(1), is(TestCoreExtensionDependency.class));
    }

    @Test
    public void resolvesDependentExtensionWhenDependantIsCreatedFirst() throws Exception
    {
        TestCoreExtension testCoreExtension = new TestCoreExtension();
        TestCoreExtensionDependency dependantCoreExtension = mock(TestCoreExtensionDependency.class);

        List<MuleCoreExtension> coreExtensions = new LinkedList<MuleCoreExtension>();
        coreExtensions.add(dependantCoreExtension);
        coreExtensions.add(testCoreExtension);

        final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<LinkedMuleCoreExtensionDependency>();
        final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency = new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class.getMethod("setTestCoreExtension", TestCoreExtension.class));
        dependencies.add(linkedMuleCoreExtensionDependency);

        when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

        final List<MuleCoreExtension> resolvedCoreExtensions = dependencyResolver.resolveDependencies(coreExtensions);

        verify(dependantCoreExtension).setTestCoreExtension(testCoreExtension);

        assertThat(resolvedCoreExtensions.size(), equalTo(2));
        assertThat(resolvedCoreExtensions.get(0), is(TestCoreExtension.class));
        assertThat(resolvedCoreExtensions.get(1), is(TestCoreExtensionDependency.class));
    }

    @Test(expected = UnresolveableDependencyException.class)
    public void throwsExceptionOnUnresolvedDependency() throws Exception
    {
        List<MuleCoreExtension> coreExtensions = new LinkedList<MuleCoreExtension>();
        TestCoreExtensionDependency dependantCoreExtension = mock(TestCoreExtensionDependency.class);
        coreExtensions.add(dependantCoreExtension);

        final List<LinkedMuleCoreExtensionDependency> dependencies = new LinkedList<LinkedMuleCoreExtensionDependency>();
        final LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency = new LinkedMuleCoreExtensionDependency(TestCoreExtension.class, TestCoreExtensionDependency.class.getMethod("setTestCoreExtension", TestCoreExtension.class));
        dependencies.add(linkedMuleCoreExtensionDependency);
        when(dependencyDiscoverer.findDependencies(dependantCoreExtension)).thenReturn(dependencies);

        dependencyResolver.resolveDependencies(coreExtensions);
    }

    public static class AbstractTestCoreExtension implements MuleCoreExtension
    {

        @Override
        public void dispose()
        {
        }

        @Override
        public void initialise() throws InitialisationException
        {
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public void start() throws MuleException
        {
        }

        @Override
        public void stop() throws MuleException
        {
        }
    }

    public static class TestCoreExtension extends AbstractTestCoreExtension
    {

    }

    public static interface TestCoreExtensionDependency extends MuleCoreExtension
    {
        @MuleCoreExtensionDependency
        void setTestCoreExtension(TestCoreExtension coreExtension);
    }
}
