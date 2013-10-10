/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
