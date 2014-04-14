/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.result.row.RowHandler;
import org.mule.module.db.internal.result.resultset.ResultSetIterator;
import org.mule.module.db.internal.result.resultset.StreamingResultSetCloser;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.ResultSet;

import org.junit.Test;

@SmallTest
public class ResultSetIteratorTestCase extends AbstractMuleTestCase
{

    private final ResultSet resultSet = mock(ResultSet.class);
    private final RowHandler rowHandler = mock(RowHandler.class);
    private final StreamingResultSetCloser streamingResultSetCloser = mock(StreamingResultSetCloser.class);
    private final DbConnection connection = mock(DbConnection.class);
    private final ResultSetIterator resultSetIterator = new ResultSetIterator(connection, resultSet, rowHandler, streamingResultSetCloser);

    @Test
    public void detectsNoMoreRecordsWhenRecordCached() throws Exception
    {
        when(resultSet.next()).thenReturn(false);
        boolean result = resultSetIterator.hasNext();

        assertThat(result, equalTo(false));
        verify(resultSet).next();
    }

    @Test
    public void detectsMoreRecordsWhenRecordCached() throws Exception
    {
        when(resultSet.next()).thenReturn(true);
        resultSetIterator.hasNext();

        boolean result = resultSetIterator.hasNext();

        assertThat(result, equalTo(true));
        verify(resultSet, times(1)).next();
    }

    @Test
    public void returnsNextRecord() throws Exception
    {
        resultSetIterator.next();

        verify(resultSet).next();
        verify(rowHandler).process(resultSet);
    }

    @Test
    public void returnsNextRecordFromCachedInvocation() throws Exception
    {
        resultSetIterator.hasNext();
        resultSetIterator.next();

        verify(resultSet).next();
        verify(rowHandler).process(resultSet);
    }

    @Test
    public void clearsCachedInvocation() throws Exception
    {
        resultSetIterator.hasNext();
        resultSetIterator.next();
        resultSetIterator.next();

        verify(resultSet, times(2)).next();
        verify(rowHandler, times(2)).process(resultSet);
    }
}
