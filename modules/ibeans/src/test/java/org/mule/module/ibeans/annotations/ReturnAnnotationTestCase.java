/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;

import java.net.MalformedURLException;
import java.net.URL;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContainsTransformerMethods
public class ReturnAnnotationTestCase extends AbstractIBeansTestCase
{
    @IntegrationBean
    private SearchIBean search;

    @Test
    public void testReturnCallURL() throws Exception
    {
        if (isOffline(getClass().getName() + ".testReturnCallURL"))
        {
            return;
        }

        String result = search.searchGoogleAndReturnURLString("foo");
        assertNotNull(result);
        assertEquals("http://www.google.com/search?q=foo", result);

        URL url = search.searchGoogleAndReturnURL("foo");
        assertNotNull(url);
        assertEquals("http://www.google.com/search?q=foo", url.toString());
    }

    @Transformer
    public URL stringToURL(String urlString) throws MalformedURLException
    {
        return new URL(urlString);
    }
}
