/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.sql.Types.DECIMAL;
import static java.sql.Types.NUMERIC;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public class BigDecimalDbTypeTestCase extends AbstractMuleTestCase
{

    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResolvedDbType resolvedDbType;
    private final int sqlType;

    public BigDecimalDbTypeTestCase(int sqlType)
    {
        this.sqlType = sqlType;
        resolvedDbType = new ResolvedDbType(sqlType, "BigDecimalDbType");
    }

    @Parameterized.Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {DECIMAL, NUMERIC});
    }

    @Test
    public void setBigDecimalValue() throws Exception
    {
        int index = 0;
        BigDecimal bigDecimalValue = new BigDecimal(1234.1234);
        resolvedDbType.setParameterValue(statement, index, bigDecimalValue);
        verify(statement).setObject(index, bigDecimalValue, sqlType, bigDecimalValue.scale());
    }

    @Test
    public void setBigDecimalValueFromDouble() throws Exception
    {
        Double doubleValue = 1234.1234;
        verifyScale(doubleValue, 4);
    }

    @Test
    public void setBigDecimalValueFromFloat() throws Exception
    {
        Float floatValue = 1234.1234f;
        verifyScale(floatValue, 4);
    }

    @Test
    public void setBigDecimalValueFromInteger() throws Exception
    {
        Integer integerValue = 1234;
        resolvedDbType.setParameterValue(statement, 0, integerValue);
        verify(statement).setObject(anyInt(), anyObject(), anyInt());
    }

    private void verifyScale(Object value, int scale) throws Exception
    {
        final BigDecimal[] bigDecimal = new BigDecimal[1];
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                bigDecimal[0] = (BigDecimal) invocation.getArguments()[1];
                return null;
            }
        }).when(statement).setObject(anyInt(), anyObject(), anyInt(), anyInt());
        resolvedDbType.setParameterValue(statement, 0, value);
        assertThat(bigDecimal[0].scale(), is(scale));
    }
}