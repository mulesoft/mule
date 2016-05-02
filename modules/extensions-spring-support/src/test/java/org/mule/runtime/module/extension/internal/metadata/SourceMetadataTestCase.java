/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.SourceId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.construct.Flow;

import com.google.common.reflect.TypeToken;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class SourceMetadataTestCase extends MetadataExtensionFunctionalTestCase
{

    private static final String TYPE_PARAMETER_NAME = "type";

    @Before
    public void setUp()
    {
        componentId = new SourceId(SOURCE_METADATA);
    }

    @Test
    public void getSourceMetadataKeys()
    {
        final MetadataResult<List<MetadataKey>> metadataKeysResult = metadataManager.getMetadataKeys(componentId);
        assertThat(metadataKeysResult.isSuccess(), is(true));
        final List<MetadataKey> metadataKeys = metadataKeysResult.get();
        assertThat(metadataKeys.size(), is(3));
        assertThat(metadataKeys.stream().map(MetadataKey::getId).collect(toList()), hasItems(PERSON, CAR, HOUSE));
    }

    @Test
    public void injectComposedMetadataKeyIdInstanceInSource() throws Exception
    {
        ((Flow) getFlowConstruct(SOURCE_METADATA_WITH_MULTILEVEL)).start();
    }

    @Test
    public void getSourceDynamicOutputMetadata() throws Exception
    {
        final ComponentMetadataDescriptor componentMetadata = getComponentDynamicMetadata(personKey);
        assertExpectedOutput(componentMetadata.getOutputMetadata(), personType, String.class);
    }

    @Test
    public void sourceDoesNotSupportDynamicContentMetadata() throws Exception
    {
        final ComponentMetadataDescriptor componentMetadata = getComponentDynamicMetadata(personKey);
        assertThat(componentMetadata.getContentMetadata().isPresent(), is(false));
    }

    @Test
    public void getSourceStaticOutputMetadata() throws IOException
    {

        final ComponentMetadataDescriptor componentMetadata = getComponentStaticMetadata();
        assertExpectedOutput(componentMetadata.getOutputMetadata(), new TypeToken<Map<String, Object>>()
        {
        }.getType(), String.class);
    }

    @Test
    public void getSourceParametersStaticMetadata() throws IOException
    {
        final ComponentMetadataDescriptor componentMetadata = getComponentStaticMetadata();
        final List<TypeMetadataDescriptor> parametersMetadata = componentMetadata.getParametersMetadata();

        assertThat(parametersMetadata.size(), is(1));
        final TypeMetadataDescriptor typeMetadataDescriptor = parametersMetadata.get(0);
        assertExpectedType(typeMetadataDescriptor, TYPE_PARAMETER_NAME, String.class);
    }

    @Test
    public void sourceDoesNotSupportStaticContentMetadata()
    {
        final ComponentMetadataDescriptor componentMetadata = getComponentStaticMetadata();
        assertThat(componentMetadata.getContentMetadata().isPresent(), is(false));
    }
}
