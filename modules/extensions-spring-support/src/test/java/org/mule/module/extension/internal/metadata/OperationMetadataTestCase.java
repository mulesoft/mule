/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.resolving.MetadataResult;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.extension.api.metadata.NullMetadataKey;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;

import java.util.List;

import org.junit.Test;

public class OperationMetadataTestCase extends MetadataExtensionFunctionalTestCase
{

    private static final String MESSAGE_ATTRIBUTES_personType_METADATA = "messageAttributesPersonTypeMetadata";
    private static final String MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA = "messageAttributesNullTypeMetadata";

    @Test
    public void getMetadataKeysWithKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeys = metadataManager.getMetadataKeys(processorId);
        assertThat(metadataKeys.isSuccess(), is(true));
        assertThat(metadataKeys.get().size(), is(3));
    }

    @Test
    public void getMetadataKeysWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeys = metadataManager.getMetadataKeys(processorId);
        assertThat(metadataKeys.isSuccess(), is(true));
        assertThat(metadataKeys.get().size(), is(1));
        assertThat(metadataKeys.get().get(0), instanceOf(NullMetadataKey.class));
    }

    @Test
    public void dynamicOperationMetadata() throws Exception
    {
        processorId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void staticOperationMetadata() throws Exception
    {
        processorId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationStaticMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);
    }

    @Test
    public void dynamicOutputWithoutContentParam() throws Exception
    {
        // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyParam
        processorId = new ProcessorId(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(false));
    }

    @Test
    public void dynamicContentWithoutOutput() throws Exception
    {
        // Resolver for content and output type, no return type, resolves only @Content, with key and KeyParam
        processorId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void operationOutputWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);

        assertThat(metadataDescriptor.getParametersMetadata(), is(empty()));
    }

    @Test
    public void contentAndOutputMetadataWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);

        assertThat(metadataDescriptor.getParametersMetadata(), is(empty()));
    }

    @Test
    public void contentMetadataWithoutKeysWithKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void outputMetadataWithoutKeysWithKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(false));
    }

    @Test
    public void messageAttributesNullTypeMetadata() throws Exception
    {
        processorId = new ProcessorId(MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationStaticMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), ExtensionsTestUtils.TYPE_BUILDER.anyType().build(), void.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void messageAttributesStringTypeMetadata() throws Exception
    {
        processorId = new ProcessorId(MESSAGE_ATTRIBUTES_personType_METADATA, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, String.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void getContentMetadataWithKey() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void getOutputMetadataWithKey() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);
    }

    @Test
    public void dynamicContentWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata(nullMetadataKey);

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void dynamicOutputWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata(nullMetadataKey);

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }
}
