/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * Will look for the href file on the classpath
 */
public class LocalURIResolver implements URIResolver
{
    // The xsl file provided by user
    private String xslFile = null;

    public LocalURIResolver()
    {
        super();
    }

    public LocalURIResolver(String xslFile)
    {
        super();
        this.xslFile = xslFile;
    }

    @Override
    public Source resolve(String href, String base) throws javax.xml.transform.TransformerException
    {
        try
        {
            InputStream is = IOUtils.getResourceAsStream(href, getClass());
            if (is != null)
            {
                return new StreamSource(is);
            }
            else if (xslFile != null)
            {
                // Try to use relative path
                int pathPos = xslFile.lastIndexOf('/');
                if (pathPos > -1)
                {
                    // Path exists
                    String path = xslFile.substring(0, pathPos + 1);
                    return new StreamSource(IOUtils.getResourceAsStream(path + href, getClass()));
                }
            }
            throw new TransformerException("Stylesheet not found: " + href);

        }
        catch (IOException e)
        {
            throw new TransformerException(e);
        }
    }
}
