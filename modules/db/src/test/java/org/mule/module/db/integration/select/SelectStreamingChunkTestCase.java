/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.module.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.module.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.module.db.integration.TestRecordUtil.getVenusRecord;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStreamingChunkTestCase extends AbstractDbIntegrationTestCase
{

    public SelectStreamingChunkTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-streaming-chunk-config.xml"};
    }

    @Test
    public void chunksStreamedRecords() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://selectStreamingChunks", TEST_MESSAGE, null);

        List chunks = (List) response.getPayload();
        assertEquals(2, chunks.size());
        assertThat(chunks.get(0), is(instanceOf(List.class)));
        assertRecords(chunks.get(0), getVenusRecord(), getEarthRecord());
        assertThat(chunks.get(1), is(instanceOf(List.class)));
        assertRecords(chunks.get(1), getMarsRecord());
    }
}
