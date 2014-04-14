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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

@SmallTest
public class CompositeDbTypeManagerTestCase extends AbstractMuleTestCase
{

    public static final int TYPE_ID = 1;
    public static final String TYPE_NAME = "";

    private final DbConnection connection = mock(DbConnection.class);
    private final DbTypeManager dbTypeManager1 = mock(DbTypeManager.class);
    private final DbTypeManager dbTypeManager2 = mock(DbTypeManager.class);
    private final List<DbTypeManager> typeManagers = Arrays.asList(dbTypeManager1, dbTypeManager2);
    private final CompositeDbTypeManager composite = new CompositeDbTypeManager(typeManagers);
    private final DbType dbType = mock(DbType.class);

    @Test
    public void lookupsByNameAndId() throws Exception
    {
        when(dbTypeManager1.lookup(connection, TYPE_ID, TYPE_NAME)).thenThrow(new UnknownDbTypeException(TYPE_ID, TYPE_NAME));
        when(dbTypeManager2.lookup(connection, TYPE_ID, TYPE_NAME)).thenReturn(dbType);

        DbType lookup = composite.lookup(connection, TYPE_ID, TYPE_NAME);

        verify(dbTypeManager1).lookup(connection, TYPE_ID, TYPE_NAME);
        assertThat(lookup, sameInstance(dbType));
    }

    @Test(expected = UnknownDbTypeException.class)
    public void failsWhenNoManagerResolvesLookupByNameAndId() throws Exception
    {
        when(dbTypeManager1.lookup(connection, TYPE_ID, TYPE_NAME)).thenThrow(new UnknownDbTypeException(TYPE_NAME));
        when(dbTypeManager2.lookup(connection, TYPE_ID, TYPE_NAME)).thenThrow(new UnknownDbTypeException(TYPE_NAME));

        composite.lookup(connection, TYPE_ID, TYPE_NAME);
    }

    @Test
    public void lookupsByName() throws Exception
    {
        when(dbTypeManager1.lookup(connection, TYPE_NAME)).thenThrow(new UnknownDbTypeException(TYPE_NAME));
        when(dbTypeManager2.lookup(connection, TYPE_NAME)).thenReturn(dbType);

        DbType lookup = composite.lookup(connection, TYPE_NAME);

        verify(dbTypeManager1).lookup(connection, TYPE_NAME);
        assertThat(lookup, sameInstance(dbType));
    }

    @Test(expected = UnknownDbTypeException.class)
    public void failsWhenNoManagerResolvesLookupByName() throws Exception
    {
        when(dbTypeManager1.lookup(connection, TYPE_NAME)).thenThrow(new UnknownDbTypeException(TYPE_NAME));
        when(dbTypeManager2.lookup(connection, TYPE_NAME)).thenThrow(new UnknownDbTypeException(TYPE_NAME));

        composite.lookup(connection, TYPE_NAME);
    }
}
