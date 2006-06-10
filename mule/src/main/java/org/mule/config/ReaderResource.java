/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config;

import org.mule.MuleManager;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * <code>ReaderResource</code> is a reader with a description associated with
 * it. This is useful for error resolution as the reader description can be
 * included when reporting errors during reading the resource.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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

    public static ReaderResource[] parseResources(String configResources, String encoding) throws IOException {
        String[] resources = StringUtils.split(configResources, ",");
        MuleManager.getConfiguration().setConfigResources(resources);
        ReaderResource[] readers = new ReaderResource[resources.length];
        for (int i = 0; i < resources.length; i++) {
            InputStream is = FileUtils.loadResource(resources[i].trim(), ReaderResource.class);
            if(is==null) {
                throw new IOException("could not load resource: " + resources[i].trim());
            }
            readers[i] = new ReaderResource(resources[i].trim(), new InputStreamReader(is, encoding));
        }
        return readers;
    }

    public static ReaderResource[] parseResources(String configResources) throws IOException {
        return parseResources(configResources, MuleManager.getConfiguration().getEncoding());
    }
}
