/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractDiscoverableTransformer;
import org.mule.transformer.types.DataTypeFactory;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Converts an RSS data representation into a SyndFeed object
 */
public class ObjectToRssFeed extends AbstractDiscoverableTransformer
{
    public ObjectToRssFeed()
    {
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(Document.class));
        registerSourceType(DataTypeFactory.create(InputSource.class));
        registerSourceType(DataTypeFactory.create(File.class));
        setReturnDataType(DataTypeFactory.create(SyndFeed.class));
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        SyndFeedInput feedInput = new SyndFeedInput();
        SyndFeed feed = null;
        try
        {
            if (src instanceof String)
            {
                feed = feedInput.build(new StringReader(src.toString()));

            }
            else if (src instanceof InputStream)
            {
                feed = feedInput.build(new XmlReader((InputStream) src));

            }
            else if (src instanceof byte[])
            {
                feed = feedInput.build(new XmlReader(new ByteArrayInputStream((byte[]) src)));

            }
            else if (src instanceof Document)
            {
                feed = feedInput.build((Document) src);

            }
            else if (src instanceof InputSource)
            {
                feed = feedInput.build((InputSource) src);

            }
            else if (src instanceof File)
            {
                feed = feedInput.build((File) src);

            }
            return feed;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }
}
