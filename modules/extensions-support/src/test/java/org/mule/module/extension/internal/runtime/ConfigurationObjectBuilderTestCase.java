/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getResolver;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.module.extension.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationObjectBuilderTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "configName";
    private static final String NAME_VALUE = "name";
    private static final String DESCRIPTION_VALUE = "description";

    @Mock
    private Extension extension;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Configuration configuration;

    @Mock
    private ConfigurationInstanceRegistrationCallback registrationCallback;

    @Mock
    private MuleEvent event;

    private TestConfig configurationInstance;

    private ConfigurationObjectBuilder configurationObjectBuilder;
    private ResolverSet resolverSet;
    private Parameter nameParameter = getParameter("name", String.class);
    private Parameter descriptionParameter = getParameter("description", String.class);

    @Before
    public void before() throws Exception
    {
        configurationInstance = new TestConfig();

        when(configuration.getParameters()).thenReturn(Arrays.asList(nameParameter, descriptionParameter));
        when(configuration.getInstantiator().newInstance()).thenReturn(configurationInstance);
        when(configuration.getInstantiator().getObjectType()).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return TestConfig.class;
            }
        });
        when(configuration.getCapabilities(ParameterGroupCapability.class)).thenReturn(null);

        resolverSet = new ResolverSet();
        resolverSet.add(nameParameter, getResolver(NAME_VALUE));
        resolverSet.add(descriptionParameter, getResolver(DESCRIPTION_VALUE));

        configurationObjectBuilder = new ConfigurationObjectBuilder(CONFIG_NAME, extension, configuration, resolverSet, registrationCallback);
    }

    @Test
    public void build() throws Exception
    {
        TestConfig testConfig = (TestConfig) configurationObjectBuilder.build(event);
        assertThat(testConfig.getName(), is(NAME_VALUE));
        assertThat(testConfig.getDescription(), is(DESCRIPTION_VALUE));
    }

    @Test
    public void registerOnBuild() throws Exception
    {
        configurationObjectBuilder.build(event);
        verify(registrationCallback).registerConfigurationInstance(extension, CONFIG_NAME, configurationInstance);
    }

    public static class TestConfig
    {

        private String name;
        private String description;

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }
    }
}
