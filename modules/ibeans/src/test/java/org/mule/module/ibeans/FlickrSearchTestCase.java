/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import org.mule.ibeans.flickr.FlickrIBean;
import org.mule.ibeans.flickr.FlickrSearchIBean;
import org.mule.module.json.JsonData;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.codehaus.jackson.node.ArrayNode;
import org.ibeans.annotation.IntegrationBean;
import org.ibeans.api.CallException;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * This tests that we can use the FlickrSearch iBean without needing a 'secret_key' which is required for other
 * parts of the Flickr API
 */
public class FlickrSearchTestCase extends AbstractMuleContextTestCase
{
    public static final String SEARCH_TERM = "food";
    public static final String BAD_SEARCH_TERM = "bad";

    @IntegrationBean
    private FlickrSearchIBean flickr;

    public FlickrSearchTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        //Add the test case so that the IntegrationBean DI will be processed        
        muleContext.getRegistry().registerObject("test", this);
        //getFlickr().init("${flickr.api.key}","${flickr.secret.key}", FlickrIBean.FORMAT.XML, Document.class);
        getFlickr().init("3a690a103c6eabf55de5b10623021a34", FlickrIBean.FORMAT.JSON, JsonData.class);

    }

    protected FlickrSearchIBean getFlickr()
    {
        return flickr;
    }

    @Test
    public void testFlickrSearch() throws Exception
    {
        JsonData doc = getFlickr().search(SEARCH_TERM);
        assertNotNull(doc);
        assertEquals(10, ((ArrayNode) doc.get("/photos/photo")).size());
    }

    //This will fail since "badkey" is not a recognised key
    @Test
    public void testFlickrError() throws Exception
    {
        getFlickr().init("badkey", FlickrIBean.FORMAT.XML, Document.class);

        try
        {
            getFlickr().search(BAD_SEARCH_TERM);
        }
        catch (CallException e)
        {
            //Flickr error code
            assertEquals("100", e.getErrorCode());
        }
    }
}