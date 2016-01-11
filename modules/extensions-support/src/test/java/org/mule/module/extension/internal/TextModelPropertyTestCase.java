/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.introspection.property.TextModelProperty;
import org.mule.module.extension.internal.introspection.enricher.TextModelEnricher;
import org.mule.module.extension.internal.runtime.connector.secure.SecureConnector;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TextModelPropertyTestCase extends AbstractAnnotatedParameterModelEnricherTest
{

    @Test
    public void verifyTextPropertyIsPopulatedAtConfig() throws Exception
    {
        List<ParameterModel> parameterModels = getExtensionModel().getConfigurationModels().get(0).getParameterModels();
        assertThat(parameterModels, hasSize(2));
        assertThat(parameterModels.get(0).getModelProperty(TextModelProperty.KEY), is(notNullValue()));
        assertThat(parameterModels.get(1).getModelProperty(TextModelProperty.KEY), is(nullValue()));

    }

    @Test
    public void verifyTextPropertyIsPopulatedAtProvider() throws Exception
    {
        List<ParameterModel> parameterModels = getExtensionModel().getConnectionProviders().get(0).getParameterModels();
        assertThat(parameterModels.get(0).getModelProperty(TextModelProperty.KEY), is(nullValue()));
        assertThat(parameterModels.get(1).getModelProperty(TextModelProperty.KEY), is(notNullValue()));
    }


    @Test
    public void verifyTextPropertyIsPopulatedAtMethodParameter() throws Exception
    {
        List<ParameterModel> parameterModels = getExtensionModel().getOperationModels().get(0).getParameterModels();
        assertThat(parameterModels, hasSize(2));
        assertThat(parameterModels.get(0).getModelProperty(TextModelProperty.KEY), is(nullValue()));
        assertThat(parameterModels.get(1).getModelProperty(TextModelProperty.KEY), is(notNullValue()));
    }

    @Override
    protected Class<?> getExtensionForTest()
    {
        return SecureConnector.class;
    }

    @Override
    protected ModelEnricher getModelEnricherUnderTest()
    {
        return new TextModelEnricher();
    }
}
