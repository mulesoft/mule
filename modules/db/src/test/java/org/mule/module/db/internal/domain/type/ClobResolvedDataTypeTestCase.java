/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.sql.Types.CLOB;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mule.module.db.internal.domain.type.ClobResolvedDataType.createUnsupportedTypeErrorMessage;
import org.mule.config.ReaderInputStream;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ClobResolvedDataTypeTestCase extends AbstractMuleTestCase {

  private static final int PARAM_INDEX = 1;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ClobResolvedDataType dataType;
  private PreparedStatement statement;
  private Connection connection;

  @Before
  public void setUp() throws Exception {
    dataType = new ClobResolvedDataType(CLOB, null);
    statement = mock(PreparedStatement.class);
    connection = mock(Connection.class);
    Clob clob = mock(Clob.class);

    when(statement.getConnection()).thenReturn(connection);
    when(connection.createClob()).thenReturn(clob);
  }

  @Test
  public void convertsStringToClobWhenDriverSupportsSetClob() throws Exception {
    String value = "foo";

    dataType.setParameterValue(statement, PARAM_INDEX, value);

    verify(connection).createClob();
  }

  @Test
  public void convertsInputStreamToClobWhenDriverSupportsSetClob() throws Exception {
    String streamContent = "bar";
    InputStream value = new StringInputStream(streamContent);
    when(connection.createClob().setCharacterStream(anyLong())).thenReturn(new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {

      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {

      }
    });

    dataType.setParameterValue(statement, PARAM_INDEX, value);

    verify(connection, atLeastOnce()).createClob();
  }

  @Test
  public void convertsStringToClobWhenDriverDoesNotSupportSetClob() throws Exception {
    when(connection.createClob()).thenThrow(new SQLException());

    String value = "foo";

    dataType.setParameterValue(statement, PARAM_INDEX, value);

    verify(statement).setCharacterStream(eq(PARAM_INDEX), any(StringReader.class), eq(value.length()));
  }

  @Test
  public void convertsInputStreamToClobWhenDriverDoesNotSupportSetClob() throws Exception {
    when(connection.createClob()).thenThrow(new SQLException());

    String streamContent = "bar";
    InputStream value = new StringInputStream(streamContent);

    dataType.setParameterValue(statement, PARAM_INDEX, value);

    verify(statement).setCharacterStream(eq(PARAM_INDEX), any(StringReader.class), eq(streamContent.length()));
  }

  @Test
  public void setClobDirectly() throws Exception
  {
    Clob clob = mock(Clob.class);

    dataType.setParameterValue(statement, PARAM_INDEX, clob);

    verify(statement).setObject(PARAM_INDEX, clob, CLOB);
  }

  @Test
  public void failsToConvertUnsupportedType() throws Exception {
    Object value = new Object();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString(createUnsupportedTypeErrorMessage(value)));

    dataType.setParameterValue(statement, PARAM_INDEX, value);
  }

  private static class StringInputStream extends ReaderInputStream
  {
    public StringInputStream(String source) {
      super(new StringReader(source));
    }
  }
}
