/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.sql.Types.STRUCT;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.db.internal.domain.type.StructDbType.createUnsupportedTypeErrorMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class StructDbTypeTestCase extends AbstractMuleTestCase
{

    private static final int PARAM_INDEX = 1;
    private static final String TYPE_NAME = "testStruct";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private StructDbType dataType;
    private PreparedStatement statement;
    private Connection connection;
    private Struct struct;

    @Before
    public void setUp() throws Exception
    {
        dataType = new StructDbType(STRUCT, TYPE_NAME);
        statement = mock(PreparedStatement.class);
        connection = mock(Connection.class);
        struct = mock(Struct.class);

        when(statement.getConnection()).thenReturn(connection);
    }

    @Test
    public void convertsArrayToStruct() throws Exception
    {
        Object[] value = new Object[] {"foo", "bar"};

        when(connection.createStruct(TYPE_NAME, value)).thenReturn(struct);

        dataType.setParameterValue(statement, PARAM_INDEX, value);

        verify(statement).setObject(PARAM_INDEX, struct, STRUCT);
    }

    @Test
    public void convertsListToStruct() throws Exception
    {
        List value = new ArrayList<>();
        value.add("foo");
        value.add("bar");

        when(connection.createStruct(argThat(equalTo(TYPE_NAME)), argThat(arrayContaining("foo", "bar")))).thenReturn(struct);

        dataType.setParameterValue(statement, PARAM_INDEX, value);

        verify(statement).setObject(PARAM_INDEX, struct, STRUCT);
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
