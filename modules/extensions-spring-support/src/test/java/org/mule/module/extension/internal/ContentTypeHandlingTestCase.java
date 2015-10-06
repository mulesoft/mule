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
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.nio.charset.Charset;

import org.junit.Rule;
import org.junit.Test;

public class ContentTypeHandlingTestCase extends ExtensionsFunctionalTestCase
{

    private static final String MIME_TYPE = "text/plain";
    private static final String ENCODING;

    static
    {
        // pick an encoding which is not the default
        ENCODING = Charset.defaultCharset().name().equals("UTF-8") ? "ISO-8859-1" : "UTF-8";
    }

    @Rule
    public SystemProperty encodingProperty = new SystemProperty("testEncoding", ENCODING);

    @Override
    protected String getConfigFile()
    {
        return "content-type-handling-config.xml";
    }

    @Test
    public void setsContentTypeOnXml() throws Exception
    {
        MuleEvent response = runFlow("setsContentTypeOnXml");
        assertThat(response.getMessage().getEncoding(), is(ENCODING));
        assertThat(response.getMessage().getMimeType(), is(MIME_TYPE));
    }

    @Test
    public void unmodifiedContentType() throws Exception
    {
        MuleEvent requestEvent = getTestEvent("");
        MuleEvent response = runFlow("defaultContentType", requestEvent);
        assertThat(response.getMessage().getEncoding(), is(requestEvent.getMessage().getEncoding()));
        assertThat(response.getMessage().getMimeType(), is(requestEvent.getMessage().getMimeType()));
    }

    @Test
    public void setsContentTypeProgrammatically() throws Exception
    {
        String lastSupportedEncoding = Charset.availableCharsets().keySet().stream().reduce((first, last) -> last).get();
        MuleEvent response = runFlow("setsContentTypeProgrammatically");

        assertThat(response.getMessage().getMimeType(), is("dead/dead"));
        assertThat(response.getMessage().getEncoding(), is(lastSupportedEncoding));
    }
}
