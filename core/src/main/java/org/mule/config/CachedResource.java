/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.springframework.core.io.AbstractResource;

/**
 * Spring 2.x is picky about open/closed input streams, as it requires a closed
 * stream (fully read resource) to enable automatic validation detection (DTD or
 * XSD). Otherwise, a caller has to specify the mode explicitly. <p/> Code relying on
 * Spring 1.2.x behavior may now break with
 * {@link org.springframework.beans.factory.BeanDefinitionStoreException}. This
 * class is called in to remedy this and should be used instead of, e.g.
 * {@link org.springframework.core.io.InputStreamResource}. <p/> The resource is
 * fully stored in memory.
 */
public class CachedResource extends AbstractResource
{

    private static final String DEFAULT_DESCRIPTION = "cached in-memory resource";

    private final byte[] buffer;
    private final String description;

    public CachedResource(byte[] source)
    {
        this(source, null);
    }

    public CachedResource(String source, String encoding) throws UnsupportedEncodingException
    {
        this(source.trim().getBytes(encoding), DEFAULT_DESCRIPTION);
    }

    public CachedResource(byte[] source, String description)
    {
        this.buffer = source;
        this.description = description;
    }

    public CachedResource(Reader reader, String encoding) throws IOException
    {
        this(IOUtils.toByteArray(reader, encoding), DEFAULT_DESCRIPTION);
    }

    public String getDescription()
    {
        return (description == null) ? "" : description;
    }

    public InputStream getInputStream() throws IOException
    {
        // This HAS to be a new InputStream, otherwise SAX
        // parser breaks with 'Premature end of file at line -1"
        // This behavior is not observed with Spring pre-2.x
        return new ByteArrayInputStream(buffer);
    }
}
