/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.transformer.types;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.SimpleDataType;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
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
        MuleContext muleContext = mock(MuleContext.class);
        when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));

        DataType<?> dataType = DataTypeFactory.createFromObject(
                new DefaultMuleMessage("test", null, null, null, muleContext, new SimpleDataType<>(String.class, "text/plain")));

        assertThat(dataType, like(String.class, "text/plain", null));
    }

    @Test
    public void mimeTypeWithEncodingInformation() throws Exception
    {
        final Class<String> type = String.class;
        final String encoding = "UTF-16";
        final String mimeType = "application/json";

        DataType dataType = DataTypeFactory.create(type, format("%s; charset=UTF-8", mimeType), encoding);
        assertThat(dataType.getType(), equalTo(type));
        assertThat(dataType.getEncoding(), is(encoding));
        assertThat(dataType.getMimeType(), is(mimeType));
    }
}