/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.delete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.DefinedMapMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataModel;
import org.mule.common.metadata.datatype.DataType;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractSingleQueryDbMessageProcessor;

import java.util.List;

public abstract class AbstractUpdateInputMetadataTestCase extends AbstractDbIntegrationTestCase
{

    public AbstractUpdateInputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    protected void doResolvedMetadataTest(String flowName)
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);
        Result<MetaData> inputMetaData = queryMessageProcessor.getInputMetaData();

        DefinedMapMetaDataModel mapDataModel = (DefinedMapMetaDataModel) inputMetaData.get().getPayload();
        assertThat(mapDataModel.getKeys().size(), equalTo(2));
        MetaDataModel id = mapDataModel.getValueMetaDataModel("position");
        assertThat(id.getDataType(), equalTo(testDatabase.getIdFieldInputMetaDataType()));
        MetaDataModel data = mapDataModel.getValueMetaDataModel("name");
        assertThat(data.getDataType(), equalTo(DataType.STRING));
    }
}
