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
import org.mule.common.metadata.MetaData;
import org.mule.construct.Flow;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractDbMessageProcessor;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DeleteBulkInputMetadataTestCase extends AbstractBulkUpdateInputMetadataTestCase
{

    public DeleteBulkInputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/delete/delete-bulk-input-metadata-config.xml"};
    }

    @Test
    public void returnsNullDeleteMetadataUnParameterizedQuery() throws Exception
    {
        doUnresolvedMetadataTest("deleteBulkMetadataNoParams");
    }

    @Test
    public void returnsNullDeleteInputMetadataFromNotSupportedParameterizedQuery() throws Exception
    {
        doUnresolvedMetadataTest("deleteBulkMetadataNotSupportedValueParams");
    }

    @Test
    public void returnsDeleteInputMetadataFromBeanParameterizedQuery() throws Exception
    {
        doResolvedMetadataTest("deleteBulkMetadataBeanParams");
    }

    @Test
    public void returnsDeleteInputMetadataFromMapParameterizedQuery() throws Exception
    {
        doResolvedMetadataTest("deleteBulkMetadataMapParams");
    }

    private void doUnresolvedMetadataTest(String flowName)
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractDbMessageProcessor messageProcessor = (AbstractDbMessageProcessor) messageProcessors.get(0);
        Result<MetaData> inputMetaData = messageProcessor.getInputMetaData();

        assertThat(inputMetaData, equalTo(null));
    }

}
