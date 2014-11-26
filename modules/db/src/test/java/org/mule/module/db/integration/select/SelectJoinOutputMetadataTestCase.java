/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mule.module.db.internal.metadata.SelectMetadataProvider.DUPLICATE_COLUMN_LABEL_ERROR;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.DefaultListMetaDataModel;
import org.mule.common.metadata.DefinedMapMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractSingleQueryDbMessageProcessor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectJoinOutputMetadataTestCase extends AbstractDbIntegrationTestCase
{

    public SelectJoinOutputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/select/select-join-output-metadata-config.xml"};
    }

    @Test
    public void returnsSelectOutputMetadata() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("joinMetadata");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);
        Result<MetaData> outputMetaData = queryMessageProcessor.getOutputMetaData(null);

        DefaultListMetaDataModel listMetaDataModel = (DefaultListMetaDataModel) outputMetaData.get().getPayload();
        assertThat(ArrayList.class.getName(), equalTo(listMetaDataModel.getImplementationClass()));

        DefinedMapMetaDataModel mapDataModel = (DefinedMapMetaDataModel) listMetaDataModel.getElementModel();
        assertThat(mapDataModel.getKeys().size(), equalTo(2));
        assertThat(mapDataModel.getValueMetaDataModel("NAME"), not(isNull()));
        assertThat(mapDataModel.getValueMetaDataModel("NAME2"), not(isNull()));
    }

    @Test
    public void detectsInvalidSelectOutputMetadata() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("joinInvalidMetadata");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);

        Result<MetaData> outputMetaData = queryMessageProcessor.getOutputMetaData(null);
        assertThat(outputMetaData.getStatus(), equalTo(Result.Status.FAILURE));
        assertThat(outputMetaData.get(), nullValue());
        assertThat(outputMetaData.getMessage(), equalTo(DUPLICATE_COLUMN_LABEL_ERROR));
    }
}
