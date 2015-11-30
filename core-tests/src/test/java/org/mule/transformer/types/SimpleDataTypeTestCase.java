/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.types;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.nio.charset.UnsupportedCharsetException;

import org.junit.Test;

@SmallTest
public class SimpleDataTypeTestCase extends AbstractMuleTestCase
{

    @Test
    public void acceptsSupportedEncoding() throws Exception
    {
        SimpleDataType<Object> dataType = new SimpleDataType<>(Object.class, null);
        dataType.setEncoding(UTF_8.name());

        assertThat(dataType.getEncoding(), equalTo(UTF_8.name()));
    }

    @Test(expected = UnsupportedCharsetException.class)
    public void rejectsUnsupportedEncoding() throws Exception
    {
        SimpleDataType<Object> dataType = new SimpleDataType<>(Object.class, null);
        dataType.setEncoding("unsupportedEncoding");
    }

    @Test
    public void acceptsValidMimeType() throws Exception
    {
        SimpleDataType<Object> dataType = new SimpleDataType<>(Object.class, null);
        dataType.setMimeType(JSON);

        assertThat(dataType.getMimeType(), equalTo(JSON));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsValidMimeType() throws Exception
    {
        SimpleDataType<Object> dataType = new SimpleDataType<>(Object.class, null);
        dataType.setMimeType("invalidMimeType");
    }
}