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
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.OperationModel;
import org.mule.extension.introspection.ParameterModel;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extension.internal.config.ExtensionConfig;
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
    private ExtensionModel extensionModel1;

    @Mock
    private ExtensionModel extensionModel2;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConfigurationModel extension1ConfigurationModel;

    @Mock
    private OperationModel extension1OperationModel;

    @Mock
    private OperationContextAdapter extension1OperationContext;

    @Mock
    private ConfigurationProvider<Object> extension1ConfigurationProvider;

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
        when(muleContext.getConfiguration().getExtension(ExtensionConfig.class)).thenReturn(null);
        extensionsManager.initialise();
        this.extensionsManager = extensionsManager;

        when(extensionModel1.getName()).thenReturn(EXTENSION1_NAME);
        when(extensionModel1.getConfigurations()).thenReturn(Arrays.asList(extension1ConfigurationModel));
        when(extensionModel2.getName()).thenReturn(EXTENSION2_NAME);

        when(extensionModel1.getVersion()).thenReturn(EXTENSION1_VERSION);
        when(extensionModel2.getVersion()).thenReturn(EXTENSION2_VERSION);

        when(extension1ConfigurationModel.getName()).thenReturn(EXTENSION1_CONFIG_NAME);
        when(extension1ConfigurationModel.getInstantiator().newInstance()).thenReturn(configInstance);
        when(extension1ConfigurationModel.getInstantiator().getObjectType()).thenReturn((Class) configInstance.getClass());

        when(extensionModel1.getConfiguration(EXTENSION1_CONFIG_NAME)).thenReturn(extension1ConfigurationModel);
        when(extensionModel1.getOperation(EXTENSION1_OPERATION_NAME)).thenReturn(extension1OperationModel);
        when(extension1OperationModel.getName()).thenReturn(EXTENSION1_OPERATION_NAME);

        when(extension1OperationContext.getOperationModel()).thenReturn(extension1OperationModel);

        when(extension1ConfigurationProvider.get(same(extension1OperationContext))).thenReturn(configInstance);

        when(extension1OperationModel.getExecutor()).thenReturn(executor);

        classLoader = getClass().getClassLoader();
        setDiscoverableExtensions(extensionModel1, extensionModel2);

        when(discoverer.discover(same(classLoader))).thenAnswer(invocation -> getTestExtensions());

        ExtensionsTestUtils.stubRegistryKeys(muleContext, EXTENSION1_CONFIG_INSTANCE_NAME, EXTENSION1_OPERATION_NAME, EXTENSION1_NAME);
    }

    private void setDiscoverableExtensions(ExtensionModel... extensionModels)
    {
        when(discoverer.discover(same(classLoader))).thenReturn(Arrays.asList(extensionModels));
    }

    @Test
    public void discover()
    {
        List<ExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
        verify(discoverer).discover(same(classLoader));
        testEquals(getTestExtensions(), extensionModels);
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
        when(extensionModel1.isCapableOf(XmlCapability.class)).thenReturn(true);
        when(extensionModel2.isCapableOf(XmlCapability.class)).thenReturn(false);

        discover();
        Set<ExtensionModel> extensionModels = extensionsManager.getExtensionsCapableOf(XmlCapability.class);

        assertThat(extensionModels, hasSize(1));
        testEquals(extensionModel1, extensionModels.iterator().next());
    }

    @Test
    public void noExtensionsCapableOf()
    {
        when(extensionModel1.isCapableOf(XmlCapability.class)).thenReturn(false);
        when(extensionModel2.isCapableOf(XmlCapability.class)).thenReturn(false);

        discover();
        Set<ExtensionModel> extensionModels = extensionsManager.getExtensionsCapableOf(XmlCapability.class);
        assertThat(extensionModels.isEmpty(), is(true));
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

        ExtensionModel newerExtensionModel = mock(ExtensionModel.class);
        when(newerExtensionModel.getName()).thenReturn(EXTENSION1_NAME);
        when(newerExtensionModel.getVersion()).thenReturn(NEWER_VERSION);

        setDiscoverableExtensions(newerExtensionModel, extensionModel2);

        List<ExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
        matchExtensions(extensionModels, Arrays.asList(newerExtensionModel, extensionModel2));
    }

    @Test
    public void hotUpdateOlderVersion()
    {
        discover();

        ExtensionModel olderExtensionModel = mock(ExtensionModel.class);
        when(olderExtensionModel.getName()).thenReturn(EXTENSION1_NAME);
        when(olderExtensionModel.getVersion()).thenReturn(OLDER_VERSION);

        setDiscoverableExtensions(olderExtensionModel, extensionModel2);

        List<ExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
        matchToTestExtensions(extensionModels);
    }

    @Test
    public void hotUpdateWithNoChanges()
    {
        discover();
        List<ExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
        matchToTestExtensions(extensionModels);
    }

    @Test
    public void hotUpdateWithInvalidVersion()
    {
        discover();

        ExtensionModel invalidVersionExtensionModel = mock(ExtensionModel.class);
        when(invalidVersionExtensionModel.getName()).thenReturn(EXTENSION1_NAME);
        when(invalidVersionExtensionModel.getVersion()).thenReturn("brandnew");

        List<ExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
        matchToTestExtensions(extensionModels);
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
    public void getConfigurationByName() throws Exception
    {
        discover();
        extensionsManager.registerConfigurationProvider(extensionModel1, EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationProvider);
        Object configurationInstance = extensionsManager.getConfiguration(extensionModel1, EXTENSION1_CONFIG_INSTANCE_NAME, extension1OperationContext);
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test
    public void getConfigurationThroughDefaultConfig()
    {
        discover();
        extensionsManager.registerConfigurationProvider(extensionModel1, EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationProvider);

        Object configurationInstance = extensionsManager.getConfiguration(extensionModel1, extension1OperationContext);
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test
    public void getConfigurationThroughImplicitConfiguration()
    {
        discover();
        when(extension1ConfigurationModel.getCapabilities(ParameterGroupCapability.class)).thenReturn(null);

        Object configurationInstance = extensionsManager.getConfiguration(extensionModel1, extension1OperationContext);
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
        when(extension1ConfigurationModel.getCapabilities(ParameterGroupCapability.class)).thenReturn(null);

        when(extensionModel1.getConfigurations()).thenAnswer(new Answer<List<ConfigurationModel>>()
        {
            @Override
            public List<ConfigurationModel> answer(InvocationOnMock invocation) throws Throwable
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        testLatch.release();
                        extensionsManager.getConfiguration(extensionModel1, extension1OperationContext);
                    }
                }.start();

                testLatch.await(timeout, timeUnit);
                joinerLatch.countDown();
                return Arrays.asList(extension1ConfigurationModel);
            }
        });

        Object configurationInstance = extensionsManager.getConfiguration(extensionModel1, extension1OperationContext);
        assertThat(joinerLatch.await(5, TimeUnit.SECONDS), is(true));
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test(expected = IllegalStateException.class)
    public void getOperationExecutorWithNotImplicitConfig()
    {
        makeExtension1ConfigurationNotImplicit();
        discover();

        extensionsManager.getConfiguration(extensionModel1, extension1OperationContext);
    }

    private void makeExtension1ConfigurationNotImplicit()
    {
        ParameterModel parameterModel1 = mock(ParameterModel.class);
        when(parameterModel1.isRequired()).thenReturn(true);

        when(extension1ConfigurationModel.getParameterModels()).thenReturn(Arrays.asList(parameterModel1, parameterModel1));
        when(extension1ConfigurationModel.getInstantiator().newInstance()).thenReturn(configInstance);
    }

    private List<ExtensionModel> getTestExtensions()
    {
        return ImmutableList.<ExtensionModel>builder()
                .add(extensionModel1)
                .add(extensionModel2)
                .build();
    }

    private void testEquals(Collection<ExtensionModel> expected, Collection<ExtensionModel> obtained)
    {
        assertThat(obtained.size(), is(expected.size()));
        Iterator<ExtensionModel> expectedIterator = expected.iterator();
        Iterator<ExtensionModel> obtainedIterator = expected.iterator();

        while (expectedIterator.hasNext())
        {
            assertThat(obtainedIterator.hasNext(), is(true));
            testEquals(expectedIterator.next(), obtainedIterator.next());
        }
    }

    private void testEquals(ExtensionModel expected, ExtensionModel obtained)
    {
        assertThat(obtained.getName(), equalTo(expected.getName()));
        assertThat(obtained.getVersion(), equalTo(expected.getVersion()));
    }

    private void matchToTestExtensions(List<ExtensionModel> extensionModels)
    {
        matchExtensions(extensionModels, getTestExtensions());
    }

    private void matchExtensions(List<ExtensionModel> actual, List<ExtensionModel> expected)
    {
        assertThat(actual, hasSize(expected.size()));
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }
}
