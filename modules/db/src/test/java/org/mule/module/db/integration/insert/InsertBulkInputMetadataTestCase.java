/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.insert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.MetaData;
import org.mule.construct.Flow;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.delete.AbstractBulkUpdateInputMetadataTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractDbMessageProcessor;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class InsertBulkInputMetadataTestCase extends AbstractBulkUpdateInputMetadataTestCase
{

    public InsertBulkInputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/insert/insert-bulk-input-metadata-config.xml"};
    }

    @Test
    public void returnsNullInsertMetadataUnParameterizedQuery() throws Exception
    {
        doUnresolvedMetadataTest("insertBulkMetadataNoParams");
    }

    @Test
    public void returnsNullInsertInputMetadataFromNotSupportedParameterizedQuery() throws Exception
    {
        doUnresolvedMetadataTest("insertBulkMetadataNotSupportedValueParams");
    }

    @Test
    public void returnsInsertInputMetadataFromBeanParameterizedQuery() throws Exception
    {
        doResolvedMetadataTest("insertBulkMetadataBeanParams");
    }

    @Test
    public void returnsInsertInputMetadataFromMapParameterizedQuery() throws Exception
    {
        doResolvedMetadataTest("insertBulkMetadataMapParams");
    }

    private void doUnresolvedMetadataTest(String flowName)
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractDbMessageProcessor queryMessageProcessor = (AbstractDbMessageProcessor) messageProcessors.get(0);
        Result<MetaData> inputMetaData = queryMessageProcessor.getInputMetaData();

        assertThat(inputMetaData, equalTo(null));
    }
}