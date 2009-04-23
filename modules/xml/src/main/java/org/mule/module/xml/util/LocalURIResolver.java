/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import org.mule.util.IOUtils;

import java.io.IOException;

import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * Will look for the href file on the classpath
 */
public class LocalURIResolver implements URIResolver
{
    public Source resolve(String href, String base) throws javax.xml.transform.TransformerException
    {
        try
        {
            return new StreamSource(IOUtils.getResourceAsStream(href, getClass()));
        }
        catch (IOException e)
        {
            throw new javax.xml.transform.TransformerException(e);
        }
    }
}
