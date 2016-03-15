/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.extension.api.annotation.param.ConfigName;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.model.property.RequireNameField;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigNameModelEnricherTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private DescribingContext describingContext;

    @Mock
    private ConfigurationDeclaration configurationDeclaration;

    private Field nameField;
    private final ConfigNameModelEnricher enricher = new ConfigNameModelEnricher();

    @Before
    public void before() throws Exception
    {
        when(describingContext.getDeclarationDescriptor().getDeclaration().getConfigurations()).thenReturn(asList(configurationDeclaration));
        mockImplementingProperty(TestNameAwareConfig.class);
        nameField = getAllFields(TestNameAwareConfig.class, withAnnotation(ConfigName.class)).iterator().next();
    }

    @Test
    public void addModelProperty() throws Exception
    {
        enricher.enrich(describingContext);
        ArgumentCaptor<RequireNameField> captor = ArgumentCaptor.forClass(RequireNameField.class);
        verify(configurationDeclaration).addModelProperty(captor.capture());

        RequireNameField property = captor.getValue();
        assertThat(property, is(notNullValue()));
        assertThat(property.getConfigNameField(), equalTo(nameField));
    }

    @Test
    public void configWithoutImplementingProperty() throws Exception
    {
        mockImplementingProperty(null);
        enricher.enrich(describingContext);
    }

    @Test(expected = IllegalConfigurationModelDefinitionException.class)
    public void manyAnnotatedFields()
    {
        mockImplementingProperty(TestMultipleNameAwareConfig.class);
        enricher.enrich(describingContext);
    }

    @Test(expected = IllegalConfigurationModelDefinitionException.class)
    public void annotatedFieldOfWrongType()
    {
        mockImplementingProperty(TestIllegalNameAwareConfig.class);
        enricher.enrich(describingContext);
    }

    private void mockImplementingProperty(Class<?> type)
    {
        ImplementingTypeModelProperty property = type != null ? new ImplementingTypeModelProperty(type) : null;
        when(configurationDeclaration.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(Optional.ofNullable(property));
    }

    public static class TestNameAwareConfig
    {

        @ConfigName
        private String name;

        public String getName()
        {
            return name;
        }
    }

    public static class TestMultipleNameAwareConfig
    {

        @ConfigName
        private String name;

        @ConfigName
        private String redundantName;
    }

    public static class TestIllegalNameAwareConfig
    {

        @ConfigName
        private Apple name;
    }
}
