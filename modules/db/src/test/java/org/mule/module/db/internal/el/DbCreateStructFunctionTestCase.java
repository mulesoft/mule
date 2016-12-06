/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.el;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.tck.size.SmallTest;

import java.sql.Struct;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.Test;

@SmallTest
public class DbCreateStructFunctionTestCase extends AbstractDbCreateFunctionTestCase
{

    @Test
    public void createsStructFromArray() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, structValues};

        DbConnection dbConnection = createDbConnection(true);

        Struct struct = mock(Struct.class);
        when(dbConnection.createStruct(TYPE_NAME, structValues)).thenReturn(struct);

        Object result = function.call(params, context);

        assertThat(result, Matchers.<Object>equalTo(struct));
    }

    @Test
    public void createsStructFromList() throws Exception
    {
        Object[] structValues = {"foo", "bar"};
        Object[] params = new Object[] {DB_CONFIG_NAME, TYPE_NAME, Arrays.asList(structValues)};

        DbConnection dbConnection = createDbConnection(true);

        Struct struct = mock(Struct.class);
        when(dbConnection.createStruct(TYPE_NAME, structValues)).thenReturn(struct);

        Object result = function.call(params, context);

        assertThat(result, Matchers.<Object>equalTo(struct));
    }


    @Override
    protected AbstractDbFunction createDbFunction(MuleContext muleContext)
    {
        return new DbCreateStructFunction(muleContext);
    }
}
