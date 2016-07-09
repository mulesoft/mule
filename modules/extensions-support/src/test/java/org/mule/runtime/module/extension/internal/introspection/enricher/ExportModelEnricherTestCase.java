/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExportModelEnricherTestCase extends AbstractMuleTestCase
{

    private static final String EXPORTED_RESOURCE = "META-INF/foo";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private DescribingContext describingContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionDeclarer extensionDeclarer;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionDeclaration extensionDeclaration;

    private ExportModelEnricher enricher = new ExportModelEnricher();
    private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

    @Before
    public void before()
    {
        when(describingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
        when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    }

    @Test
    public void enrich() throws Exception
    {
        setImplementingType(TestExport.class);

        enricher.enrich(describingContext);

        ArgumentCaptor<ExportModelProperty> captor = forClass(ExportModelProperty.class);
        verify(extensionDeclarer).withModelProperty(captor.capture());

        ExportModelProperty property = captor.getValue();
        assertThat(property, is(notNullValue()));

        assertThat(property.getExportedTypes(), hasItem(typeLoader.load(ExportModelEnricherTestCase.class)));
        assertThat(property.getExportedResources(), hasItem(EXPORTED_RESOURCE));
    }


    @Test
    public void shouldNotEnrich() throws Exception
    {
        setImplementingType(Object.class);
        enricher.enrich(describingContext);
        verify(extensionDeclarer, never()).withModelProperty(any());
    }

    private void setImplementingType(Class<?> type)
    {
        when(extensionDeclaration.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(Optional.of(new ImplementingTypeModelProperty(type)));
    }

    @Export(classes = {ExportModelEnricherTestCase.class}, resources = {EXPORTED_RESOURCE})
    private static class TestExport
    {

    }
}
