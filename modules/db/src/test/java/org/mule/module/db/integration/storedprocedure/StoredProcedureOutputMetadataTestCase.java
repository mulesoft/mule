/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.MetaData;
import org.mule.construct.Flow;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.internal.processor.StoredProcedureMessageProcessor;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureOutputMetadataTestCase extends AbstractDbIntegrationTestCase
{

    public StoredProcedureOutputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/storedprocedure/stored-procedure-metadata-config.xml"};
    }

    @Test
    public void returnsNullStoredProcedureOutputMetadata() throws Exception
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct("storedProcedureMetadata");

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        StoredProcedureMessageProcessor dbMessageProcessor = (StoredProcedureMessageProcessor) messageProcessors.get(0);
        Result<MetaData> inputMetaData = dbMessageProcessor.getOutputMetaData(null);

        assertThat(inputMetaData, equalTo(null));
    }
}
