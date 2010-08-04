/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.api.annotations.Transformer;

import java.net.MalformedURLException;
import java.net.URL;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

public class ReturnAnnotationTestCase extends AbstractIBeansTestCase
{
    @SuppressWarnings("unused")
    @IntegrationBean
    private SearchIBean search;

    @Test
    public void testReturnCallURL() throws Exception
    {
        String result = search.searchAskAndReturnURLString("foo");
        assertNotNull(result);
        assertEquals("http://www.ask.com/web?q=foo&search=search", result);

        URL url = search.searchAskAndReturnURL("foo");
        assertNotNull(url);
        assertEquals("http://www.ask.com/web?q=foo&search=search", url.toString());
    }

    @Transformer
    public URL stringToURL(String urlString) throws MalformedURLException
    {
        return new URL(urlString);
    }

}
