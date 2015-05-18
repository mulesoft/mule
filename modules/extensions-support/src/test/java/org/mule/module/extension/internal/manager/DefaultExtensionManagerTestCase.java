/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.api.MuleContext;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.module.extension.internal.runtime.DelegatingOperationExecutor;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private ExtensionManager extensionsManager;

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
    private OperationContext extension1OperationContext;

    @Mock
    private ConfigurationInstanceProvider<Object> extension1ConfigurationInstanceProvider;

    @Mock
    private DelegatingOperationExecutor executor;

    private ClassLoader classLoader;

    private final Object configInstance = new Object();
    private final Object executorDelegate = new Object();

    @Before
    public void before()
    {
        DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
        extensionsManager.setExtensionsDiscoverer(discoverer);
        extensionsManager.setMuleContext(muleContext);
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

        when(extension1ConfigurationInstanceProvider.getConfiguration()).thenReturn(extension1Configuration);
        when(extension1ConfigurationInstanceProvider.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);
        when(extension1ConfigurationInstanceProvider.get(same(extension1OperationContext), any(ConfigurationInstanceRegistrationCallback.class))).thenAnswer(new Answer<Object>()
        {
            private boolean firstTime = true;

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                if (firstTime)
                {
                    ConfigurationInstanceRegistrationCallback callback = (ConfigurationInstanceRegistrationCallback) invocation.getArguments()[1];
                    callback.registerNewConfigurationInstance(extension1ConfigurationInstanceProvider, configInstance);
                    firstTime = false;
                }
                return configInstance;
            }
        });

        when(executor.getExecutorDelegate()).thenReturn(executorDelegate);
        when(extension1Operation.getExecutor(configInstance)).thenReturn(executor);

        classLoader = getClass().getClassLoader();

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

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn(NEWER_VERSION);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions, hasSize(1));
        testEquals(extension1, extensions.get(0));
    }

    @Test
    public void hotUpdateOlderVersion()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn(OLDER_VERSION);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test
    public void hotUpdateWithNoChanges()
    {
        discover();
        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test
    public void hotUpdateWithInvalidVersion()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn("brandnew");

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions.isEmpty(), is(true));
    }

    @Test
    public void hotUpdateWithOneInvalidVersion()
    {
        discover();

        extension1 = mock(Extension.class);
        when(extension1.getName()).thenReturn(EXTENSION1_NAME);
        when(extension1.getVersion()).thenReturn("brandnew");

        extension2 = mock(Extension.class);
        when(extension2.getName()).thenReturn(EXTENSION2_NAME);
        when(extension2.getVersion()).thenReturn(NEWER_VERSION);

        List<Extension> extensions = extensionsManager.discoverExtensions(classLoader);
        assertThat(extensions, hasSize(1));
        testEquals(extension2, extensions.get(0));
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
    public void registerConfigurationInstanceProvider() throws Exception
    {
        discover();

        extensionsManager.registerConfigurationInstanceProvider(EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);
        initialiseIfNeeded(extensionsManager);

        assertThat(extensionsManager.getOperationExecutor(EXTENSION1_CONFIG_INSTANCE_NAME, extension1OperationContext), is(notNullValue()));
        ExtensionsTestUtils.assertRegisteredWithUniqueMadeKey(muleContext, EXTENSION1_CONFIG_INSTANCE_NAME, configInstance);
    }

    @Test
    public void getOperationExecutor() throws Exception
    {
        discover();
        extensionsManager.registerConfigurationInstanceProvider(EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);

        OperationExecutor managedExecutor = extensionsManager.getOperationExecutor(EXTENSION1_CONFIG_INSTANCE_NAME, extension1OperationContext);
        assertThat(managedExecutor, is(sameInstance((OperationExecutor) executor)));

        // ask for the same executor again and check that it's still the same instance
        managedExecutor = extensionsManager.getOperationExecutor(EXTENSION1_CONFIG_INSTANCE_NAME, extension1OperationContext);
        assertThat(managedExecutor, is(sameInstance((OperationExecutor) executor)));

        verify(muleContext.getRegistry()).registerObject(anyString(), same(executorDelegate));
        verify(extension1Operation).getExecutor(configInstance);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOperationExecutorForUnregisteredConfigurationInstanceProvider() throws Exception
    {
        discover();
        OperationContext operationContext = mock(OperationContext.class);
        when(operationContext.getOperation()).thenReturn(extension1Operation);
        ConfigurationInstanceProvider<?> configurationInstanceProvider = mock(ConfigurationInstanceProvider.class);
        when(configurationInstanceProvider.getConfiguration()).thenReturn(extension1Configuration);

        extensionsManager.getOperationExecutor(EXTENSION1_CONFIG_INSTANCE_NAME, operationContext);
    }

    @Test
    public void initialise() throws Exception
    {
        discover();
        Map<String, ConfigurationInstanceProvider> providers = new HashMap<>();
        providers.put(EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);
        when(muleContext.getRegistry().lookupByType(ConfigurationInstanceProvider.class)).thenReturn(providers);

        initialiseIfNeeded(extensionsManager);
    }

    @Test
    public void getConfigurationInstance() throws Exception
    {
        assertThat(extensionsManager, is(instanceOf(ExtensionManagerAdapter.class)));
        ExtensionManagerAdapter extensionsManager = (ExtensionManagerAdapter) this.extensionsManager;

        discover();
        extensionsManager.registerConfigurationInstanceProvider(EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);
        Object configurationInstance = extensionsManager.getConfigurationInstance(extension1ConfigurationInstanceProvider, extension1OperationContext);
        assertThat(configurationInstance, is(sameInstance(configInstance)));
    }

    @Test
    public void getOperationExecutorThroughDefaultConfig()
    {
        discover();
        extensionsManager.registerConfigurationInstanceProvider(EXTENSION1_CONFIG_INSTANCE_NAME, extension1ConfigurationInstanceProvider);

        OperationExecutor managedExecutor = extensionsManager.getOperationExecutor(extension1OperationContext);
        assertThat(managedExecutor, is(sameInstance((OperationExecutor) executor)));
        verify(extension1Operation).getExecutor(configInstance);
    }

    @Test(expected = IllegalStateException.class)
    public void getOperationExecutorWithNotImplicitConfig()
    {
        makeExtension1ConfigurationNotImplicit();
        discover();

        extensionsManager.getOperationExecutor(extension1OperationContext);
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

}
