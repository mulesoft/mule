/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.processor.MessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.MetaData;
import org.mule.construct.Flow;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.delete.AbstractUpdateInputMetadataTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.processor.AbstractSingleQueryDbMessageProcessor;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateInputMetadataTestCase extends AbstractUpdateInputMetadataTestCase
{

    public UpdateInputMetadataTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/update/update-input-metadata-config.xml"};
    }

    @Test
    public void returnsNullUpdateMetadataUnParameterizedQuery() throws Exception
    {
        doUnresolvedMetadataTest("updateMetadataNoParams");
    }

    @Test
    public void returnsNullUpdateInputMetadataFromNotSupportedParameterizedQuery() throws Exception
    {
        doUnresolvedMetadataTest("updateMetadataNotSupportedValueParams");
    }

    @Test
    public void returnsUpdateInputMetadataFromBeanParameterizedQuery() throws Exception
    {
        doResolvedMetadataTest("updateMetadataBeanParams");
    }

    @Test
    public void returnsUpdateInputMetadataFromMapParameterizedQuery() throws Exception
    {
        doResolvedMetadataTest("updateMetadataMapParams");
    }

    private void doUnresolvedMetadataTest(String flowName)
    {
        Flow flowConstruct = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);

        List<MessageProcessor> messageProcessors = flowConstruct.getMessageProcessors();
        AbstractSingleQueryDbMessageProcessor queryMessageProcessor = (AbstractSingleQueryDbMessageProcessor) messageProcessors.get(0);
        Result<MetaData> inputMetaData = queryMessageProcessor.getInputMetaData();

        assertThat(inputMetaData, equalTo(null));
    }
}