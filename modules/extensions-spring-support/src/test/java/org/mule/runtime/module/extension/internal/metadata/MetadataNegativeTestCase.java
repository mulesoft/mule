/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.MISSING_ELEMENT_ERROR_MESSAGE;

import org.mule.runtime.core.internal.metadata.InvalidComponentIdException;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.SourceId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class MetadataNegativeTestCase extends MetadataExtensionFunctionalTestCase
{

    private static final String FAIL_WITH_RESOLVING_EXCEPTION = "failWithResolvingException";
    private static final String FAIL_WITH_RUNTIME_EXCEPTION = "failWithRuntimeException";
    private static final String NON_EXISTING_FLOW = "nonExistingFlow";
    private static final String LOGGER_FLOW = "loggerFlow";
    private static final String SOURCE_NOT_FOUND = "Flow doesn't contain a message source";
    private static final String FLOW_WITHOUT_SOURCE = "flowWithoutSource";

    @Test
    public void getOperationMetadataWithResolvingException() throws Exception
    {
        componentId = new ProcessorId(FAIL_WITH_RESOLVING_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "", FailureCode.UNKNOWN, MetadataResolvingException.class.getName());
    }

    @Test
    public void getKeysWithRuntimeException() throws Exception
    {
        componentId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<List<MetadataKey>> metadata = metadataManager.getMetadataKeys(componentId);

        assertFailure(metadata, "", FailureCode.UNKNOWN, RuntimeException.class.getName());
    }

    @Test
    public void getOperationMetadataWithRuntimeException() throws Exception
    {
        componentId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "", FailureCode.UNKNOWN, RuntimeException.class.getName());
    }

    @Test
    public void flowDoesNotExist() throws Exception
    {
        componentId = new ProcessorId(NON_EXISTING_FLOW, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "Processor doesn't exist ", FailureCode.UNKNOWN, InvalidComponentIdException.class.getName());
    }

    @Test
    public void processorDoesNotExist() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, "10");
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "Processor doesn't exist", FailureCode.UNKNOWN, IndexOutOfBoundsException.class.getName());
    }

    @Test
    public void processorIsNotMetadataAware() throws Exception
    {
        componentId = new ProcessorId(LOGGER_FLOW, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "not MetadataAware", FailureCode.UNKNOWN, ClassCastException.class.getName());
    }

    @Test
    public void fetchMissingElementFromCache() throws Exception
    {
        componentId = new ProcessorId(CONTENT_ONLY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, nullMetadataKey);

        assertFailure(metadata, MISSING_ELEMENT_ERROR_MESSAGE, FailureCode.UNKNOWN, MetadataResolvingException.class.getName());
    }

    @Test
    public void failToGetMetadataFromNonExistingSource() throws IOException
    {
        final SourceId notExistingSource = new SourceId(FLOW_WITHOUT_SOURCE);
        final MetadataResult<List<MetadataKey>> metadataKeysResult = metadataManager.getMetadataKeys(notExistingSource);

        assertFailure(metadataKeysResult, SOURCE_NOT_FOUND, FailureCode.UNKNOWN, InvalidComponentIdException.class.getName());
    }
}
