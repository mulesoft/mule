/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.concurrent.Latch;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExtensionManagerTestCase extends AbstractMuleTestCase
{

    private ExtensionManagerAdapter extensionsManager;

    private static final String EXTENSION1_NAME = "extension1";
    private static final String EXTENSION1_CONFIG_NAME = "extension1Config";
    private static final String EXTENSION1_CONFIG_INSTANCE_NAME = "extension1ConfigInstanceName";
    private static final String EXTENSION1_OPERATION_NAME = "extension1OperationName";
    private static final String EXTENSION2_NAME = "extension2";
    private static final String EXTENSION1_VERSION = "3.6.0";
    private static final String EXTENSION2_VERSION = "3.6.0";
    private static final String NEWER_VERSION = "4.0";
    private static final String OLDER_VERSION = "3.5.1";

    @Mock
    private ExtensionDiscoverer discoverer;

    @Mock
    private Extension extension1;

    @Mock
    private Extension extension2;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Configuration extension1Configuration;

    @Mock
    private Operation extension1Operation;

    @Mock
    private OperationContextAdapter extension1OperationContext;

    @Mock
    private ConfigurationInstanceProvider<Object> extension1ConfigurationInstanceProvider;

    @Mock
    private OperationExecutor executor;

    private ClassLoader classLoader;

    private final Object configInstance = new Object();

    @Before
    public void before() throws InitialisationException
    {
        DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
        extensionsManager.setExtensionsDiscoverer(discoverer);
        extensionsManager.setMuleContext(muleContext);
        extensionsManager.initialise();
        this.extensionsManager = extensionsManager;

        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getConfigurations()).thenReturn(Arrays.asList(extension1Configuration));
        when(extension2.getName()).thenReturn(EXTENSION2_NAME);

        when(extension1.getVersion()).thenReturn(EXTENSION1_VERSION);
        when(extension2.getVersion()).thenReturn(EXTENSION2_VERSION);

        when(extension1Configuration.getName()).thenReturn(EXTENSION1_CONFIG_NAME);
        when(extension1Configuration.getInstantiator().newInstance()).thenReturn(configInstance);
        when(extension1Configuration.getInstantiator().getObjectType()).thenReturn((Class) configInstance.getClass());

        when(extension1.getConfiguration(EXTENSION1_CONFIG_NAME)).thenReturn(extension1Configuration);
        when(extension1.getOperation(EXTENSION1_OPERATION_NAME)).thenReturn(extension1Operation);
        when(extension1Operation.getName()).thenReturn(EXTENSION1_OPERATION_NAME);

        when(extension1OperationContext.getOperation()).thenReturn(extension1Operation);

        when(extension1ConfigurationInstanceProvider.get(same(extension1OperationContext))).thenReturn(configInstance);

        when(extension1Operation.getExecutor()).thenReturn(executor);

        classLoader = getClass().getClassLoader();
        setDiscoverableExtensions(extension1, extension2);

        when(discoverer.discover(same(classLoader))).thenAnswer(new Answer<List<Extension>>()
        {
            @Override
            public List<Extension> answer(InvocationOnMock invocation) throws Throwable
            {
                return getTestExtensions();
            }
        });

        ExtensionsTestUtils.stubRegistryKeys(muleContext, EXTENSION1_CONFIG_INSTANCE_NAME, EXTENSION1_OPERATION_NAME, EXTENSION1_NAME);
    }

    private void setDiscoverableExtensions(Extension... extensions)
    {
        when(discoverer.discover(same(classLoader))).thenReturn(Arrays.asList(extensions));
    }

    @Test
    public void discover()
    {
        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        verify(discoverer).discover(same(classLoader));
        testEquals(getTestExtensions(), extensions);
    }

    @Test
    public void getExtensions()
    {
        discover();
        testEquals(getTestExtensions(), extensionsManager.getExtensions());
    }

    @Test
    public void getExtensionsCapableOf()
    {
        when(extension1.isCapableOf(XmlCapability.class)).thenReturn(true);
        when(extension2.isCapableOf(XmlCapability.class)).thenReturn(false);

        discover();
        Set<Extension> extensions = extensionsManager.getExtensionsCapableOf(XmlCapability.class);

        assertThat(extensions, hasSize(1));
        testEquals(extension1, extensions.iterator().next());
    }

    @Test
    public void noExtensionsCapableOf()
    {
        when(extension1.isCapableOf(XmlCapability.class)).thenReturn(false);
        when(extension2.isCapableOf(XmlCapability.class)).thenReturn(false);

        discover();
        Set<Extension> extensions = extensionsManager.getExtensionsCapableOf(XmlCapability.class);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extensionsCapableOfNull()
    {
        extensionsManager.getExtensionsCapableOf(null);
    }

    @Test
    public void hotUpdateNewerExtension()
    {
        discover();

        Extension newerExtension = mock(Extension.class);
        when(newerExtension.getName()).thenReturn(EXTENSION1_NAME);
        when(newerExtension.getVersion()).thenReturn(NEWER_VERSION);

        setDiscoverableExtensions(newerExtension, extension2);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        matchExtensions(extensions, Arrays.asList(newerExtension, extension2));
    }

    @Test
    public void hotUpdateOlderVersion()
    {
        discover();

        Extension olderExtension = mock(Extension.class);
        when(olderExtension.getName()).thenReturn(EXTENSION1_NAME);
        when(olderExtension.getVersion()).thenReturn(OLDER_VERSION);

        setDiscoverableExtensions(olderExtension, extension2);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        matchToTestExtensions(extensions);
    }

    @Test
    public void hotUpdateWithNoChanges()
    {
        discover();
        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        matchToTestExtensions(extensions);
    }

    @Test
    public void hotUpdateWithInvalidVersion()
    {
        discover();

        Extension invalidVersionExtension = mock(Extension.class);
        when(invalidVersionExtension.getName()).thenReturn(EXTENSION1_NAME);
        when(invalidVersionExtension.getVersion()).thenReturn("brandnew");

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        matchToTestExtensions(extensions);
    }

    @Test
    public void contextClassLoaderKept()
    {
        discover();
        assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
    }

    @Test
    public void contextClassLoaderKeptAfterException()
    {
        when(discoverer.discover(same(classLoader))).thenThrow(RuntimeException.class);
        try
        {
            discover();
            fail("was expecting an exception");
        }
        catch (RuntimeException e)
        {
            assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
        }
    }

    @Test
    public void getConfigurationInstanceByName() throws Exception
    {
        discover();
        extensionsManager.registerConfigurationInstanceProvider(extension1, EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);
        Object configurationInstance = extensionsManager.getConfigurationInstance(extension1, EXTENSION1_CONFIG_INSTANCE_NAME, extension1OperationContext);
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test
    public void getConfigurationInstanceThroughDefaultConfig()
    {
        discover();
        extensionsManager.registerConfigurationInstanceProvider(extension1, EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);

        Object configurationInstance = extensionsManager.getConfigurationInstance(extension1, extension1OperationContext);
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test
    public void getConfigurationInstanceThroughImplicitConfiguration()
    {
        discover();
        when(extension1Configuration.getCapabilities(ParameterGroupCapability.class)).thenReturn(null);

        Object configurationInstance = extensionsManager.getConfigurationInstance(extension1, extension1OperationContext);
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test
    public void getOperationExecutorThroughImplicitConfigurationConcurrently() throws Exception
    {
        final long timeout = 5;
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final int threadCount = 2;
        final Latch testLatch = new Latch();
        final CountDownLatch joinerLatch = new CountDownLatch(threadCount);

        discover();
        when(extension1Configuration.getCapabilities(ParameterGroupCapability.class)).thenReturn(null);

        when(extension1.getConfigurations()).thenAnswer(new Answer<List<Configuration>>()
        {
            @Override
            public List<Configuration> answer(InvocationOnMock invocation) throws Throwable
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        testLatch.release();
                        extensionsManager.getConfigurationInstance(extension1, extension1OperationContext);
                    }
                }.start();

                testLatch.await(timeout, timeUnit);
                joinerLatch.countDown();
                return Arrays.asList(extension1Configuration);
            }
        });

        Object configurationInstance = extensionsManager.getConfigurationInstance(extension1, extension1OperationContext);
        assertThat(joinerLatch.await(5, TimeUnit.SECONDS), is(true));
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test(expected = IllegalStateException.class)
    public void getOperationExecutorWithNotImplicitConfig()
    {
        makeExtension1ConfigurationNotImplicit();
        discover();

        extensionsManager.getConfigurationInstance(extension1, extension1OperationContext);
    }

    private void makeExtension1ConfigurationNotImplicit()
    {
        Parameter parameter1 = mock(Parameter.class);
        when(parameter1.isRequired()).thenReturn(true);

        when(extension1Configuration.getParameters()).thenReturn(Arrays.asList(parameter1, parameter1));
        when(extension1Configuration.getInstantiator().newInstance()).thenReturn(configInstance);
    }

    private List<Extension> getTestExtensions()
    {
        return ImmutableList.<Extension>builder()
                .add(extension1)
                .add(extension2)
                .build();
    }

    private void testEquals(Collection<Extension> expected, Collection<Extension> obtained)
    {
        assertThat(obtained.size(), is(expected.size()));
        Iterator<Extension> expectedIterator = expected.iterator();
        Iterator<Extension> obtainedIterator = expected.iterator();

        while (expectedIterator.hasNext())
        {
            assertThat(obtainedIterator.hasNext(), is(true));
            testEquals(expectedIterator.next(), obtainedIterator.next());
        }
    }

    private void testEquals(Extension expected, Extension obtained)
    {
        assertThat(obtained.getName(), equalTo(expected.getName()));
        assertThat(obtained.getVersion(), equalTo(expected.getVersion()));
    }

    private void matchToTestExtensions(List<Extension> extensions)
    {
        matchExtensions(extensions, getTestExtensions());
    }

    private void matchExtensions(List<Extension> actual, List<Extension> expected)
    {
        assertThat(actual, hasSize(expected.size()));
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }
}
