/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Test;

@SmallTest
public class StaticDbTypeManagerTestCase extends AbstractMuleTestCase
{

    private final DbConnection connection = mock(DbConnection.class);
    private final StaticDbTypeManager typeManager = new StaticDbTypeManager(Arrays.asList(JdbcTypes.VARCHAR_DB_TYPE));

    @Test
    public void resolvesByName() throws Exception
    {
        DbType lookup = typeManager.lookup(connection, JdbcTypes.VARCHAR_DB_TYPE.getName());

        assertThat(lookup, sameInstance(JdbcTypes.VARCHAR_DB_TYPE));
    }

    @Test(expected = UnknownDbTypeException.class)
    public void failsWhenNoTypeDefined() throws Exception
    {
        typeManager.lookup(connection, "NonRegisteredType");
    }

    @Test(expected = UnknownDbTypeException.class)
    public void doesNotResolveByNameAndId() throws Exception
    {
        typeManager.lookup(connection, 0, JdbcTypes.VARCHAR_DB_TYPE.getName());
    }
}
