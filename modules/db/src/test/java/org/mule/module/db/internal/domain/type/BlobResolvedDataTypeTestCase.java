/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.sql.Types.BLOB;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.domain.type.BlobResolvedDataType.createUnsupportedTypeErrorMessage;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class BlobResolvedDataTypeTestCase extends AbstractMuleTestCase
{

    private static final int PARAM_INDEX = 1;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private BlobResolvedDataType dataType;
    private PreparedStatement statement;
    private Connection connection;
    private Blob blob;

    @Before
    public void setUp() throws Exception
    {
        dataType = new BlobResolvedDataType(BLOB, null);
        statement = mock(PreparedStatement.class);
        connection = mock(Connection.class);
        blob = mock(Blob.class);

        when(statement.getConnection()).thenReturn(connection);
        when(connection.createBlob()).thenReturn(blob);
    }

    @Test
    public void convertsStringToBlob() throws Exception
    {
        String value = "foo";

        dataType.setParameterValue(statement, PARAM_INDEX, value.getBytes());

        verify(blob).setBytes(1, value.getBytes());
        verify(statement).setObject(PARAM_INDEX, blob, BLOB);
    }

    @Test
    public void convertsInputStreamToBlob() throws Exception
    {
        String streamContent = "bar";
        InputStream value = new ByteArrayInputStream(streamContent.getBytes());

        dataType.setParameterValue(statement, PARAM_INDEX, value);

        verify(blob).setBytes(1, streamContent.getBytes());
        verify(statement).setObject(PARAM_INDEX, blob, BLOB);
    }

    @Test
    public void failsToConvertUnsupportedType() throws Exception
    {

        Object value = new Object();
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(containsString(createUnsupportedTypeErrorMessage(value)));

        dataType.setParameterValue(statement, PARAM_INDEX, value);
    }
}
