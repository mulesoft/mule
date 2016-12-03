/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.sql.Struct;

import org.junit.Test;

public class DbCreateStructTestCase extends AbstractDbFunctionTestCase
{

    public DbCreateStructTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/function/create-struct-udt-config.xml"};
    }

    @Test
    public void createsStruct() throws Exception
    {
        Object[] payload = SOUTHWEST_MANAGER.getContactDetails().asObjectArray();

        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://createsStruct", payload, null);

        assertThat(((Struct) response.getPayload()).getAttributes(), equalTo(SOUTHWEST_MANAGER.getContactDetails().asObjectArray()));
    }
}
