/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.SourceId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.InvalidComponentIdException;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

public class MetadataNegativeTestCase extends MetadataExtensionFunctionalTestCase
{

    private static final String FAIL_WITH_RESOLVING_EXCEPTION = "failWithResolvingException";
    private static final String FAIL_WITH_RUNTIME_EXCEPTION = "failWithRuntimeException";
    private static final String FAIL_KEYS_RETRIEVER = "Failing keysResolver retriever";
    private static final String NON_EXISTING_FLOW = "nonExistingFlow";
    private static final String LOGGER_FLOW = "loggerFlow";
    private static final String SOURCE_NOT_FOUND = "Flow doesn't contain a message source";
    private static final String FLOW_WITHOUT_SOURCE = "flowWithoutSource";

    @Override
    protected String getConfigFile()
    {
        return METADATA_TEST;
    }

    @Test
    public void getOperationMetadataWithResolvingException() throws Exception
    {
        componentId = new ProcessorId(FAIL_WITH_RESOLVING_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);
        assertFailure(metadata, "", FailureCode.MULTIPLE, "");
    }

    @Test
    public void getKeysWithRuntimeException() throws Exception
    {
        componentId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<Set<MetadataKey>> metadata = metadataManager.getMetadataKeys(componentId);

        assertFailure(metadata, "", FailureCode.UNKNOWN, RuntimeException.class.getName());
    }

    @Test
    public void getOperationMetadataWithRuntimeException() throws Exception
    {
        componentId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "", FailureCode.MULTIPLE, "");
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
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID, "10");
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId, personKey);

        assertFailure(metadata, "Processor doesn't exist", FailureCode.UNKNOWN, IndexOutOfBoundsException.class.getName());
    }

    @Test
    public void failToGetMetadataFromNonExistingSource() throws IOException
    {
        final SourceId notExistingSource = new SourceId(FLOW_WITHOUT_SOURCE);
        final MetadataResult<Set<MetadataKey>> metadataKeysResult = metadataManager.getMetadataKeys(notExistingSource);

        assertFailure(metadataKeysResult, SOURCE_NOT_FOUND, FailureCode.UNKNOWN, InvalidComponentIdException.class.getName());
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
        assertFailure(metadata, "", FailureCode.UNKNOWN, "");
    }

    @Test
    public void failWithDynamicConfigurationWhenRetrievingMetadata() throws IOException
    {
        componentId = new ProcessorId(RESOLVER_WITH_DYNAMIC_CONFIG, FIRST_PROCESSOR_INDEX);
        MetadataKey key = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SAN_FRANCISCO, CITY))).build();
        MetadataResult<ComponentMetadataDescriptor> result = metadataManager.getMetadata(componentId, key);
        assertFailure(result, "Configuration used for Metadata fetch cannot be dynamic", FailureCode.INVALID_CONFIGURATION, MetadataResolvingException.class.getName());
    }

    @Test
    public void failToGetMetadataWithMissingMetadataKeyLevels() throws Exception
    {
        componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
        final MetadataKey metadataKey = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY)).build();

        final MetadataResult<ComponentMetadataDescriptor> metadataResult = metadataManager.getMetadata(componentId, metadataKey);
        assertFailure(metadataResult, "", FailureCode.UNKNOWN, "");
    }
}
