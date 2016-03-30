/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataKeyBuilder;
import org.mule.api.metadata.MetadataManager;
import org.mule.api.metadata.MuleMetadataManager;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.api.metadata.resolving.FailureCode;
import org.mule.api.metadata.resolving.MetadataResult;
import org.mule.extension.api.metadata.NullMetadataKey;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.module.extension.internal.metadata.extension.MetadataConnection;
import org.mule.module.extension.internal.metadata.extension.MetadataExtension;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

public abstract class MetadataExtensionFunctionalTestCase extends ExtensionFunctionalTestCase
{

    protected static final String FIRST_PROCESSOR_INDEX = "0";

    protected static final String CONTENT_METADATA_WITH_KEY_PARAM = "contentMetadataWithKeyParam";
    protected static final String OUTPUT_METADATA_WITH_KEY_PARAM = "outputMetadataWithKeyParam";
    protected static final String CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM = "contentAndOutputMetadataWithKeyParam";
    protected static final String OUTPUT_ONLY_WITHOUT_CONTENT_PARAM = "outputOnlyWithoutContentParam";
    protected static final String CONTENT_ONLY_IGNORES_OUTPUT = "contentOnlyIgnoresOutput";
    protected static final String CONTENT_METADATA_WITHOUT_KEY_PARAM = "contentMetadataWithoutKeyParam";
    protected static final String OUTPUT_METADATA_WITHOUT_KEY_PARAM = "outputMetadataWithoutKeyParam";
    protected static final String CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_PARAM = "contentAndOutputMetadataWithoutKeyParam";
    protected static final String CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM = "contentMetadataWithoutKeysWithKeyParam";
    protected static final String OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM = "outputMetadataWithoutKeysWithKeyParam";

    protected final NullMetadataKey nullMetadataKey = new NullMetadataKey();
    protected MetadataType personType;

    protected MetadataKey personKey;
    protected ProcessorId processorId;
    protected MuleEvent event;
    protected MetadataManager metadataManager;
    protected ClassTypeLoader typeLoader;

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {MetadataExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "metadata-tests.xml";
    }

    @Before
    public void setContext() throws Exception
    {
        event = getTestEvent("");
        metadataManager = muleContext.getRegistry().lookupObject(MuleMetadataManager.class);
    }

    @Before
    public void setMetadata() throws Exception
    {
        typeLoader = ExtensionsTestUtils.TYPE_LOADER;
        personKey = MetadataKeyBuilder.newKey(MetadataConnection.PERSON).build();
        personType = TestMetadataUtils.getMetadata(personKey);
    }

    protected OperationMetadataDescriptor getOperationDynamicMetadata()
    {
        return getOperationDynamicMetadata(personKey);
    }

    protected OperationMetadataDescriptor getOperationDynamicMetadata(MetadataKey key)
    {
        MetadataResult<OperationMetadataDescriptor> operationMetadata = metadataManager.getMetadata(processorId, key);
        assertThat(operationMetadata.isSuccess(), is(true));

        return operationMetadata.get();
    }

    protected OperationMetadataDescriptor getOperationStaticMetadata()
    {
        MetadataResult<OperationMetadataDescriptor> operationMetadata = metadataManager.getMetadata(processorId);
        assertThat(operationMetadata.isSuccess(), is(true));

        return operationMetadata.get();
    }

    protected void assertFailure(MetadataResult<?> result, String msgContains, FailureCode failureCode, String traceContains) throws IOException
    {
        assertThat(result.isSuccess(), is(false));
        assertThat(result.getFailure().get().getFailureCode(), is(failureCode));

        if (!StringUtils.isBlank(msgContains))
        {
            assertThat(result.getFailure().get().getMessage(), containsString(msgContains));
        }

        if (!StringUtils.isBlank(traceContains))
        {
            assertThat(result.getFailure().get().getReason(), containsString(traceContains));
        }
    }

    protected void assertExpectedOutput(OutputMetadataDescriptor outputDescriptor, Class<?> payloadType, Class<?> attributesType) throws IOException
    {
        assertExpectedType(outputDescriptor.getPayloadMetadata(), "Message.Payload", payloadType);
        assertExpectedType(outputDescriptor.getAttributesMetadata(), "Message.Attributes", attributesType);
    }

    protected void assertExpectedOutput(OutputMetadataDescriptor outputDescriptor, MetadataType payloadType, Class<?> attributesType) throws IOException
    {
        assertExpectedType(outputDescriptor.getPayloadMetadata(), "Message.Payload", payloadType);
        assertExpectedType(outputDescriptor.getAttributesMetadata(), "Message.Attributes", attributesType);
    }

    protected void assertExpectedType(TypeMetadataDescriptor param, String name, Class<?> type) throws IOException
    {
        if (!StringUtils.isBlank(name))
        {
            assertThat(param.getName(), is(name));
        }
        assertThat(param.getType(), is(typeLoader.load(type)));
    }

    protected void assertExpectedType(TypeMetadataDescriptor param, String name, MetadataType type) throws IOException
    {
        if (!StringUtils.isBlank(name))
        {
            assertThat(param.getName(), is(name));
        }
        assertThat(param.getType(), is(type));
    }

}
