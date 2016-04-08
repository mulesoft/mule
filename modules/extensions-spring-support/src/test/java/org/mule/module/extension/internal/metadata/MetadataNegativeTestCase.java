/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.mule.module.extension.internal.metadata.extension.resolver.TestResolverWithCache.MISSING_ELEMENT_ERROR_MESSAGE;
import org.mule.api.metadata.InvalidComponentIdException;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.resolving.FailureCode;
import org.mule.api.metadata.resolving.MetadataResult;

import java.util.List;

import org.junit.Test;

public class MetadataNegativeTestCase extends MetadataExtensionFunctionalTestCase
{

    private static final String FAIL_WITH_RESOLVING_EXCEPTION = "failWithResolvingException";
    private static final String FAIL_WITH_RUNTIME_EXCEPTION = "failWithRuntimeException";
    private static final String NON_EXISTING_FLOW = "nonExistingFlow";
    private static final String LOGGER_FLOW = "loggerFlow";

    @Test
    public void getOperationMetadataWithResolvingException() throws Exception
    {
        processorId = new ProcessorId(FAIL_WITH_RESOLVING_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<OperationMetadataDescriptor> metadata = metadataManager.getMetadata(processorId, personKey);

        assertFailure(metadata, "", FailureCode.UNKNOWN, MetadataResolvingException.class.getName());
    }

    @Test
    public void getKeysWithRuntimeException() throws Exception
    {
        processorId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<List<MetadataKey>> metadata = metadataManager.getMetadataKeys(processorId);

        assertFailure(metadata, "", FailureCode.UNKNOWN, RuntimeException.class.getName());
    }

    @Test
    public void getOperationMetadataWithRuntimeException() throws Exception
    {
        processorId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<OperationMetadataDescriptor> metadata = metadataManager.getMetadata(processorId, personKey);

        assertFailure(metadata, "", FailureCode.UNKNOWN, RuntimeException.class.getName());
    }

    @Test
    public void flowDoesNotExist() throws Exception
    {
        processorId = new ProcessorId(NON_EXISTING_FLOW, FIRST_PROCESSOR_INDEX);
        MetadataResult<OperationMetadataDescriptor> metadata = metadataManager.getMetadata(processorId, personKey);

        assertFailure(metadata, "Processor doesn't exist ", FailureCode.UNKNOWN, InvalidComponentIdException.class.getName());
    }

    @Test
    public void processorDoesNotExist() throws Exception
    {
        processorId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, "10");
        MetadataResult<OperationMetadataDescriptor> metadata = metadataManager.getMetadata(processorId, personKey);

        assertFailure(metadata, "Processor doesn't exist", FailureCode.UNKNOWN, IndexOutOfBoundsException.class.getName());
    }

    @Test
    public void processorIsNotMetadataAware() throws Exception
    {
        processorId = new ProcessorId(LOGGER_FLOW, FIRST_PROCESSOR_INDEX);
        MetadataResult<OperationMetadataDescriptor> metadata = metadataManager.getMetadata(processorId, personKey);

        assertFailure(metadata, "not MetadataAware", FailureCode.UNKNOWN, ClassCastException.class.getName());
    }

    @Test
    public void fetchMissingElementFromCache() throws Exception
    {
        processorId = new ProcessorId(CONTENT_ONLY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
        MetadataResult<OperationMetadataDescriptor> metadata = metadataManager.getMetadata(processorId, nullMetadataKey);

        assertFailure(metadata, MISSING_ELEMENT_ERROR_MESSAGE, FailureCode.UNKNOWN, MetadataResolvingException.class.getName());
    }


}
