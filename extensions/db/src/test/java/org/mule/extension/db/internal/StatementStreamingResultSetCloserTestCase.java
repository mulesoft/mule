/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.ResultSet;

import org.junit.After;
import org.junit.Test;

@SmallTest
public class StatementStreamingResultSetCloserTestCase extends AbstractMuleTestCase {

  private final DbConnection connection = mock(DbConnection.class);
  private final StatementStreamingResultSetCloser resultSetCloser = new StatementStreamingResultSetCloser(connection);
  private final ResultSet resultSet1 = mock(ResultSet.class);
  private final ResultSet resultSet2 = mock(ResultSet.class);

  @After
  public void after() {
    assertThat(resultSetCloser.getOpenResultSets(), is(0));
  }

  @Test
  public void closesRegisteredResultSet() throws Exception {
    resultSetCloser.trackResultSet(resultSet1);

    resultSetCloser.closeResultSets();

    verify(resultSet1).close();
  }

  @Test
  public void tracksMultipleResultSetFromConnection() throws Exception {
    resultSetCloser.trackResultSet(resultSet1);
    resultSetCloser.trackResultSet(resultSet2);

    resultSetCloser.closeResultSets();

    verify(resultSet1).close();
    verify(resultSet2).close();
  }
}
