/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.RegistryContext;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * <code>ReaderResource</code> is a reader with a description associated with it.
 * This is useful for error resolution as the reader description can be included when
 * reporting errors during reading the resource.
 */
public class ReaderResource
{

    private String description;
    private Reader reader;

    public ReaderResource(String description, Reader reader)
    {
        this.description = description;
        this.reader = reader;
    }

    public String getDescription()
    {
        return description;
    }

    public Reader getReader()
    {
        return reader;
    }

    public static ReaderResource[] parseResources(String configResources, String encoding) throws IOException
    {
        String[] resources = StringUtils.splitAndTrim(configResources, ",");
        RegistryContext.getConfiguration().setConfigResources(resources);
        ReaderResource[] readers = new ReaderResource[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            InputStream is = IOUtils.getResourceAsStream(resources[i].trim(), ReaderResource.class);
            if (is == null)
            {
                throw new IOException("could not load resource: " + resources[i].trim());
            }
            readers[i] = new ReaderResource(resources[i].trim(), new InputStreamReader(is, encoding));
        }
        return readers;
    }
}
