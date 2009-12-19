/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.atom.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractDiscoverableTransformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;

/**
 * <code>ObjectToInputStream</code> converts serilaizable object to a input stream but
 * treats <code>java.lang.String</code> differently by converting to bytes using
 * the <code>String.getBytes()</code> method.
 */
public class ObjectToFeed extends AbstractDiscoverableTransformer
{

    public ObjectToFeed()
    {
        this.registerSourceType(byte[].class);
        this.registerSourceType(InputStream.class);
        this.registerSourceType(String.class);
        setReturnClass(Feed.class);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            Parser parser = Abdera.getInstance().getParser();
            Document<Element> doc;
            if (src instanceof InputStream)
            {
                doc = parser.parse((InputStream) src, encoding);
            }
            else if (src instanceof byte[])
            {
                doc = parser.parse(new ByteArrayInputStream((byte[]) src), encoding);
            }
            else
            {
                doc = parser.parse(new StringReader((String) src));
            }
            //we only need to check for the registered source types

            return doc.getRoot();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
