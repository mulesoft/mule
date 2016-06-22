/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

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

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
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
        assertCustomEncoding(dataType);
        assertThat(dataType.getMimeType(), is(MIME_TYPE));
    }

    @Test
    public void onlySetEncodingOnXml() throws Exception
    {
        MuleEvent response = runFlow("onlySetEncodingOnXml");
        DataType dataType = response.getMessage().getDataType();
        assertCustomEncoding(dataType);
        assertCustomMimeType(dataType);
    }

    @Test
    public void onlySetMimeTypeOnXml() throws Exception
    {
        MuleEvent response = runFlow("onlySetMimeTypeOnXml");
        DataType dataType = response.getMessage().getDataType();
        assertDefaultEncoding(dataType);
        assertCustomMimeType(dataType);
    }

    @Test
    public void maintainsContentType() throws Exception
    {
        MuleEvent response = flowRunner("defaultContentType").withPayload("").run();
        final DataType responseDataType = response.getMessage().getDataType();

        assertDefaultEncoding(responseDataType);
        assertDefaultMimeType(responseDataType);
    }

    @Test
    public void setEncodingInMimeTypeAndParam() throws Exception
    {
        MuleEvent response = runFlow("setEncodingInMimeTypeAndParam");
        DataType dataType = response.getMessage().getDataType();
        assertThat(dataType.getEncoding(), is("UTF-16"));
        assertThat(dataType.getMimeType(), is("application/json"));
    }

    @Test
    public void overridesContentType() throws Exception
    {
        String lastSupportedEncoding = Charset.availableCharsets().keySet().stream().reduce((first, last) -> last).get();
        MuleEvent response = runFlow("setsContentTypeProgrammatically");

        final DataType dataType = response.getMessage().getDataType();
        assertCustomMimeType(dataType);
        assertThat(dataType.getEncoding(), is(lastSupportedEncoding));
    }

    private void assertCustomMimeType(DataType dataType)
    {
        assertThat(dataType.getMimeType(), is("dead/dead"));
    }

    private void assertCustomEncoding(DataType dataType)
    {
        assertThat(dataType.getEncoding(), is(customEncoding));
    }

    private void assertDefaultEncoding(DataType dataType) throws Exception
    {
        assertThat(getDefaultDataType().getEncoding(), is(dataType.getEncoding()));
    }

    private void assertDefaultMimeType(DataType dataType) throws Exception
    {
        assertThat(getDefaultDataType().getMimeType(), is(dataType.getMimeType()));
    }

    private DataType getDefaultDataType()
    {
        FlowRunner runner = flowRunner("defaultContentType").withPayload("");
        return runner.buildEvent().getMessage().getDataType();
    }
}
