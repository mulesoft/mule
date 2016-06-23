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
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MimeType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DataTypeFactoryTestCase extends AbstractMuleTestCase
{

    @Test
    public void createsDataTypeForNullObject() throws Exception
    {
        DataType<?> dataType = DataType.of(null);

        assertThat(dataType, like(Object.class, MimeType.ANY, null));
    }

    @Test
    public void createsDataTypeForNonNullObject() throws Exception
    {
        DataType<?> dataType = DataType.of("test");

        assertThat(dataType, like(String.class, MimeType.ANY, null));
    }

    @Test
    public void mimeTypeWithEncodingInformation() throws Exception
    {
        final Class<String> type = String.class;
        final String encoding = "UTF-16";
        final String mimeType = "application/json";

        DataType<?> dataType = DataType.builder().type(type).mimeType(format("%s; charset=UTF-8", mimeType)).encoding(encoding).build();
        assertThat(dataType.getType(), equalTo(type));
        assertThat(dataType.getEncoding(), is(encoding));
        assertThat(dataType.getMimeType(), is(mimeType));
    }
}
