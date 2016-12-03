/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mule.module.db.integration.model.Contact.CONTACT2;
import static org.mule.module.db.integration.model.Region.NORTHWEST;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.sql.Struct;

import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class DbCreateArrayFunctionTestCase extends AbstractDbFunctionTestCase
{

    public DbCreateArrayFunctionTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/function/create-array-udt-config.xml"};
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        final DataSource dataSource = getDefaultDataSource();
        testDatabase.createStoredProcedureGetZipCodes(dataSource);
        testDatabase.createStoredProcedureGetContactDetails(dataSource);
        testDatabase.createStoredProcedureUpdateZipCodes(dataSource);
        testDatabase.createStoredProcedureUpdateContactDetails(dataSource);
    }

    @Test
    public void createsDefaultTypeArray() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://createsDefaultTypeArray", NORTHWEST.getZips(), null);

        assertThat(response.getPayload(), Matchers.<Object>equalTo(NORTHWEST.getZips()));
    }

    @Test
    public void createsCustomTypeArray() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://createsCustomTypeArray", CONTACT2.getDetails(), null);

        Object[] arrayValue = (Object[]) response.getPayload();
        assertThat(arrayValue.length, equalTo(1));
        assertThat(arrayValue[0], instanceOf(Struct.class));
        Object[] attributes = ((Struct) arrayValue[0]).getAttributes();
        assertThat(attributes, equalTo(CONTACT2.getDetailsAsObjectArray()[0]));
    }
}
