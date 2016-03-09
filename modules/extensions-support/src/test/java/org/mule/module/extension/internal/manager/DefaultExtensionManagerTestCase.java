/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExtensionDiscoverer;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.RuntimeConfigurationModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.RuntimeOperationModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.extension.api.runtime.OperationExecutorFactory;
import org.mule.module.extension.internal.config.ExtensionConfig;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExtensionManagerTestCase extends AbstractMuleTestCase
{

    private static final String MULESOFT = "MuleSoft";
    private static final String OTHER_VENDOR = "OtherVendor";
    private ExtensionManager extensionsManager;

    private static final String EXTENSION1_NAME = "extension1";
    private static final String EXTENSION1_CONFIG_NAME = "extension1Config";
    private static final String EXTENSION1_CONFIG_INSTANCE_NAME = "extension1ConfigInstanceName";
    private static final String EXTENSION1_OPERATION_NAME = "extension1OperationName";
    private static final String EXTENSION2_NAME = "extension2";
    private static final String EXTENSION1_VERSION = "3.6.0";
    private static final String EXTENSION2_VERSION = "3.6.0";

    @Mock
    private ExtensionDiscoverer discoverer;

    @Mock
    private RuntimeExtensionModel extensionModel1;

    @Mock
    private RuntimeExtensionModel extensionModel2;

    @Mock
    private RuntimeExtensionModel extensionModel3WithRepeatedName;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RuntimeConfigurationModel extension1ConfigurationModel;

    @Mock
    private RuntimeOperationModel extension1OperationModel;

    @Mock
    private OperationContextAdapter extension1OperationContext;

    @Mock
    private ConfigurationProvider<Object> extension1ConfigurationProvider;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConfigurationInstance<Object> extension1ConfigurationInstance = mock(ConfigurationInstance.class);

    @Mock
    private OperationExecutorFactory executorFactory;

    @Mock
    private OperationExecutor executor;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

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
        when(extensionModel1.getConfigurationModels()).thenReturn(asList(extension1ConfigurationModel));
        when(extensionModel2.getName()).thenReturn(EXTENSION2_NAME);
        when(extensionModel3WithRepeatedName.getName()).thenReturn(EXTENSION2_NAME);

        when(extensionModel1.getVendor()).thenReturn(MULESOFT);
        when(extensionModel2.getVendor()).thenReturn(MULESOFT);
        when(extensionModel3WithRepeatedName.getVendor()).thenReturn(OTHER_VENDOR);

        when(extensionModel1.getVersion()).thenReturn(EXTENSION1_VERSION);
        when(extensionModel2.getVersion()).thenReturn(EXTENSION2_VERSION);
        when(extensionModel3WithRepeatedName.getVersion()).thenReturn(EXTENSION2_VERSION);

        when(extension1ConfigurationModel.getName()).thenReturn(EXTENSION1_CONFIG_NAME);
        when(extension1ConfigurationModel.getConfigurationFactory().newInstance()).thenReturn(configInstance);
        when(extension1ConfigurationModel.getConfigurationFactory().getObjectType()).thenReturn((Class) configInstance.getClass());
        when(extension1ConfigurationModel.getExtensionModel()).thenReturn(extensionModel1);
        when(extension1ConfigurationModel.getInterceptorFactories()).thenReturn(emptyList());

        when(extensionModel1.getConfigurationModel(EXTENSION1_CONFIG_NAME)).thenReturn(extension1ConfigurationModel);
        when(extensionModel1.getOperationModel(EXTENSION1_OPERATION_NAME)).thenReturn(extension1OperationModel);
        when(extension1OperationModel.getName()).thenReturn(EXTENSION1_OPERATION_NAME);

        when(extension1ConfigurationInstance.getValue()).thenReturn(configInstance);
        when(extension1ConfigurationInstance.getModel()).thenReturn(extension1ConfigurationModel);
        when(extension1ConfigurationInstance.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);

        when(extension1ConfigurationProvider.get(event)).thenReturn(extension1ConfigurationInstance);

        when(extension1ConfigurationProvider.getModel()).thenReturn(extension1ConfigurationModel);
        when(extension1ConfigurationProvider.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);


        when(extension1OperationModel.getExecutor()).thenReturn(executorFactory);
        when(executorFactory.createExecutor()).thenReturn(executor);

        classLoader = getClass().getClassLoader();
        setDiscoverableExtensions(extensionModel1, extensionModel2, extensionModel3WithRepeatedName);

        when(discoverer.discover(same(classLoader))).thenAnswer(invocation -> getTestExtensions());

        ExtensionsTestUtils.stubRegistryKeys(muleContext, EXTENSION1_CONFIG_INSTANCE_NAME, EXTENSION1_OPERATION_NAME, EXTENSION1_NAME);
    }

    private void setDiscoverableExtensions(RuntimeExtensionModel... extensionModels)
    {
        when(discoverer.discover(same(classLoader))).thenReturn(asList(extensionModels));
    }

    @Test
    public void discover()
    {
        List<RuntimeExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
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
        registerConfigurationProvider();

        ConfigurationInstance<Object> configurationInstance = extensionsManager.getConfiguration(EXTENSION1_CONFIG_INSTANCE_NAME, event);
        assertThat(configurationInstance.getValue(), is(sameInstance(configInstance)));
    }

    @Test
    public void getConfigurationThroughDefaultConfig() throws Exception
    {
        discover();
        registerConfigurationProvider();

        ConfigurationInstance<Object> configInstance = extensionsManager.getConfiguration(extensionModel1, event);
        assertThat(configInstance.getValue(), is(sameInstance(this.configInstance)));
    }

    @Test
    public void getConfigurationThroughImplicitConfiguration() throws Exception
    {
        discover();
        when(extension1ConfigurationModel.getModelProperty(ParameterGroupModelProperty.KEY)).thenReturn(null);
        registerConfigurationProvider();
        ConfigurationInstance<Object> configInstance = extensionsManager.getConfiguration(extensionModel1, event);
        assertThat(configInstance.getValue(), is(sameInstance(this.configInstance)));
    }

    @Test
    public void getOperationExecutorThroughImplicitConfigurationConcurrently() throws Exception
    {
        final int threadCount = 2;
        final CountDownLatch joinerLatch = new CountDownLatch(threadCount);

        MuleRegistry registry = muleContext.getRegistry();
        discover();
        when(extension1ConfigurationModel.getModelProperty(ParameterGroupModelProperty.KEY)).thenReturn(null);
        when(registry.lookupObjects(ConfigurationProvider.class)).thenReturn(emptyList());

        doAnswer(invocation -> {
            when(muleContext.getRegistry().lookupObjects(ConfigurationProvider.class)).thenReturn(asList((ConfigurationProvider) invocation.getArguments()[1]));
            new Thread(() -> extensionsManager.getConfiguration(extensionModel1, event)).start();
            joinerLatch.countDown();

            return null;
        }).when(registry).registerObject(anyString(), anyObject());

        ConfigurationInstance<Object> configurationInstance = extensionsManager.getConfiguration(extensionModel1, event);
        joinerLatch.countDown();
        assertThat(joinerLatch.await(5, TimeUnit.SECONDS), is(true));
        assertThat(configurationInstance.getValue(), is(sameInstance(configInstance)));
    }

    @Test(expected = IllegalStateException.class)
    public void getOperationExecutorWithNotImplicitConfig()
    {
        when(muleContext.getRegistry().lookupObjects(ConfigurationProvider.class)).thenReturn(emptyList());
        makeExtension1ConfigurationNotImplicit();
        discover();

        extensionsManager.getConfiguration(extensionModel1, event);
    }

    @Test
    public void registerTwoExtensionsWithTheSameNameButDifferentVendor()
    {
        setDiscoverableExtensions(extensionModel2, extensionModel3WithRepeatedName);
        List<RuntimeExtensionModel> extensionModels = extensionsManager.discoverExtensions(classLoader);
        List<String> extensionNameList = extensionModels.stream().map(ExtensionModel::getName).distinct().collect(Collectors.toList());
        List<String> extensionVendorList = extensionModels.stream().map(ExtensionModel::getVendor).distinct().collect(Collectors.toList());

        assertThat(extensionModels.size(), is(2));
        assertThat(extensionNameList.size(), is(1));
        assertThat(extensionVendorList.size(), is(2));
    }

    @Test
    public void registerSameExtensionMultipleTimesButOnlyKeepOne()
    {
        setDiscoverableExtensions(extensionModel1, extensionModel1, extensionModel1);
        assertThat(extensionsManager.discoverExtensions(classLoader).size(), is(1));
    }

    private void makeExtension1ConfigurationNotImplicit()
    {
        ParameterModel parameterModel1 = mock(ParameterModel.class);
        when(parameterModel1.isRequired()).thenReturn(true);

        when(extension1ConfigurationModel.getParameterModels()).thenReturn(asList(parameterModel1, parameterModel1));
        when(extension1ConfigurationModel.getConfigurationFactory().newInstance()).thenReturn(configInstance);
    }

    private List<RuntimeExtensionModel> getTestExtensions()
    {
        return ImmutableList.<RuntimeExtensionModel>builder()
                .add(extensionModel1)
                .add(extensionModel2)
                .add(extensionModel3WithRepeatedName)
                .build();
    }

    private void testEquals(Collection<RuntimeExtensionModel> expected, Collection<RuntimeExtensionModel> obtained)
    {
        assertThat(obtained.size(), is(expected.size()));
        Iterator<RuntimeExtensionModel> expectedIterator = expected.iterator();
        Iterator<RuntimeExtensionModel> obtainedIterator = expected.iterator();

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

    private void registerConfigurationProvider() throws RegistrationException
    {
        extensionsManager.registerConfigurationProvider(extension1ConfigurationProvider);
        verify(muleContext.getRegistry()).registerObject(extension1ConfigurationProvider.getName(), extension1ConfigurationProvider);
        when(muleContext.getRegistry().lookupObjects(ConfigurationProvider.class)).thenReturn(asList(extension1ConfigurationProvider));
        when(muleContext.getRegistry().get(extension1ConfigurationProvider.getName())).thenReturn(extension1ConfigurationProvider);
    }
}