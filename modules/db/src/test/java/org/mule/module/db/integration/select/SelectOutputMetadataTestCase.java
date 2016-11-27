/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.DefaultListMetaDataModel;
import org.mule.common.metadata.DefinedMapMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataModel;
import org.mule.common.metadata.datatype.DataType;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractSingleQueryDbMessageProcessor;
import org.mule.module.db.internal.result.resultset.ResultSetIterator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectOutputMetadataTestCase extends AbstractDbIntegrationTestCase
{

    public SelectOutputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-output-metadata-config.xml"};
    }

    @Test
    public void returnsSelectOutputMetadata() throws Exception
    {
        doSelectMetadataTest("selectMetadata", ArrayList.class.getName());
    }

    @Test
    public void returnsSelectStreamingOutputMetadata() throws Exception
    {
        doSelectMetadataTest("selectStreamingMetadata", ResultSetIterator.class.getName());
    }

    private void doSelectMetadataTest(String flowName, String implementationClass)
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);
        Result<MetaData> outputMetaData = queryMessageProcessor.getOutputMetaData(null);

        DefaultListMetaDataModel listMetaDataModel = (DefaultListMetaDataModel) outputMetaData.get().getPayload();
        assertEquals(implementationClass, listMetaDataModel.getImplementationClass());
        DefinedMapMetaDataModel mapDataModel = (DefinedMapMetaDataModel) listMetaDataModel.getElementModel();

        assertThat(mapDataModel.getKeys().size(), equalTo(4));
        MetaDataModel id = mapDataModel.getValueMetaDataModel("ID");
        assertThat(id.getDataType(), equalTo(testDatabase.getIdFieldOutputMetaDataType()));
        MetaDataModel type = mapDataModel.getValueMetaDataModel("POSITION");
        assertThat(type.getDataType(), equalTo(testDatabase.getPositionFieldOutputMetaDataType()));
        MetaDataModel data = mapDataModel.getValueMetaDataModel("NAME");
        assertThat(data.getDataType(), equalTo(DataType.STRING));
        MetaDataModel description = mapDataModel.getValueMetaDataModel("DESCRIPTION");
        assertThat(description.getDataType(), equalTo(testDatabase.getDescriptionFieldOutputMetaDataType()));
    }
}
