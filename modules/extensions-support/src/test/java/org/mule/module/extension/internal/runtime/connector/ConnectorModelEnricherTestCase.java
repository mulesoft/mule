/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.introspection.declaration.fluent.Declaration;
import org.mule.extension.runtime.Interceptor;
import org.mule.extension.runtime.InterceptorFactory;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnectorConfig;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConnectorModelEnricherTestCase extends AbstractMuleTestCase
{

    @Mock
    private Declaration declaration;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private DescribingContext describingContext;

    @Mock
    private ConfigurationDeclaration connectorConfig;

    @Mock
    private ConfigurationDeclaration notConnectorConfig;

    private ConnectorModelEnricher enricher = new ConnectorModelEnricher();

    @Before
    public void before()
    {
        when(describingContext.getDeclarationDescriptor().getDeclaration()).thenReturn(declaration);
        when(declaration.getConfigurations()).thenReturn(asList(connectorConfig, notConnectorConfig));
        when(connectorConfig.getModelProperty(ImplementingTypeModelProperty.KEY))
                .thenReturn(new ImplementingTypeModelProperty(PetStoreConnectorConfig.class));
    }

    @Test
    public void enrichConnectorConfig()
    {
        enricher.enrich(describingContext);
        ArgumentCaptor<InterceptorFactory> captor = ArgumentCaptor.forClass(InterceptorFactory.class);
        verify(connectorConfig).addInterceptorFactory(captor.capture());

        InterceptorFactory interceptorFactory = captor.getValue();
        assertThat(interceptorFactory, is(notNullValue()));
        Interceptor interceptor = interceptorFactory.createInterceptor();
        assertThat(interceptor, is(instanceOf(ConnectionInterceptor.class)));
    }

    @Test
    public void notConnectorNotEnriched()
    {
        enricher.enrich(describingContext);
        verify(notConnectorConfig).getModelProperty(ImplementingTypeModelProperty.KEY);
        verify(notConnectorConfig, never()).addInterceptorFactory(any(InterceptorFactory.class));
    }

}
