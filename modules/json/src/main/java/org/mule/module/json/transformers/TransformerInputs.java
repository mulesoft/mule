/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.json.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

public class TransformerInputs
{
    /**
     * Turn whatever we got as the transformer's source into either an input stream or a reader
     */
    private InputStream is;
    private Reader reader;

    TransformerInputs(Transformer xform, Object src) throws TransformerException
    {
        try
        {
            if (src instanceof InputStream)
            {
                is = (InputStream) src;
            }
            else if (src instanceof File)
            {
                is = new FileInputStream((File) src);
            }
            else if (src instanceof URL)
            {
                is = ((URL) src).openStream();
            }
            else if (src instanceof byte[])
            {
                is = new ByteArrayInputStream((byte[]) src);
            }
            else if (src instanceof Reader)
            {
                reader = (Reader) src;
            }
            else if (src instanceof String)
            {
                reader = new StringReader((String) src);
            }
            if (is != null || reader != null)
            {
                return;
            }
        }
        catch (IOException ex)
        {
            throw new TransformerException(xform, ex);
        }
        throw new TransformerException(
            CoreMessages.transformFailed(src.getClass().getName(), "xml"));
    }

    public InputStream getInputStream()
    {
        return is;
    }

    public Reader getReader()
    {
        return reader;
    }

}