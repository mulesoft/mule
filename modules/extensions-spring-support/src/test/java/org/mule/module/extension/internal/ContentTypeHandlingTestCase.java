/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.metadata.DataType;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

public class ContentTypeHandlingTestCase extends ExtensionFunctionalTestCase
{

    private static final String MIME_TYPE = "text/plain";

    private String customEncoding;

    @Override
    protected String getConfigFile()
    {
        return "content-type-handling-config.xml";
    }

    @Before
    public void before()
    {
        customEncoding = Charset.defaultCharset().name().equals("UTF-8") ? "ISO-8859-1" : "UTF-8";
    }

    @Test
    public void setsContentTypeOnXml() throws Exception
    {
        MuleEvent response = runFlow("setsContentTypeOnXml");
        DataType dataType = response.getMessage().getDataType();
        assertThat(dataType.getEncoding(), is(customEncoding));
        assertThat(dataType.getMimeType(), is(MIME_TYPE));
    }

    @Test
    public void maintainsContentType() throws Exception
    {
        MuleEvent requestEvent = getTestEvent("");
        MuleEvent response = runFlow("defaultContentType", requestEvent);

        final DataType requestDataType = requestEvent.getMessage().getDataType();
        final DataType responseDataType = response.getMessage().getDataType();

        assertThat(requestDataType.getEncoding(), is(responseDataType.getEncoding()));
        assertThat(requestDataType.getMimeType(), is(responseDataType.getMimeType()));
    }

    @Test
    public void overridesContentType() throws Exception
    {
        String lastSupportedEncoding = Charset.availableCharsets().keySet().stream().reduce((first, last) -> last).get();
        MuleEvent response = runFlow("setsContentTypeProgrammatically");

        final DataType dataType = response.getMessage().getDataType();
        assertThat(dataType.getMimeType(), is("dead/dead"));
        assertThat(dataType.getEncoding(), is(lastSupportedEncoding));
    }
}
