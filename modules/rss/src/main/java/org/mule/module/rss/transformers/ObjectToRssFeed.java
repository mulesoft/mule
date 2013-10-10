/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
