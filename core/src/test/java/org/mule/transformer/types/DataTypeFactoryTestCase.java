/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DataTypeFactoryTestCase extends AbstractMuleTestCase
{

    @Test
    public void createsDataTypeForNullObject() throws Exception
    {
        DataType<?> dataType = DataTypeFactory.createFromObject(null);

        assertThat(dataType, like(Object.class, MimeTypes.ANY, null));
    }

    @Test
    public void createsDataTypeForNonNullObject() throws Exception
    {
        DataType<?> dataType = DataTypeFactory.createFromObject("test");

        assertThat(dataType, like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void createsDataTypeForMessage() throws Exception
    {
        DataType<?> dataType = DataTypeFactory.createFromObject(
                new DefaultMuleMessage("test", null, null, null, mock(MuleContext.class), new SimpleDataType<>(String.class, "text/plain")));

        assertThat(dataType, like(String.class, "text/plain", null));
    }
}