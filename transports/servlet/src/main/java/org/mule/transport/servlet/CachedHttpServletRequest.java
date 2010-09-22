/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.log4j.lf5.util.StreamUtils;

public class CachedHttpServletRequest extends HttpServletRequestWrapper
{

    private CachedServletInputStream cachedServletInputStream;

    public CachedHttpServletRequest(HttpServletRequest request)
    {
        super(request);
        try
        {
            this.cachedServletInputStream = new CachedServletInputStream(request.getInputStream());
        }
        catch (IOException e)
        {
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        if (this.cachedServletInputStream != null)
        {
            return this.cachedServletInputStream;
        }
        else
        {
            return super.getInputStream();
        }
    }

    private static class CachedServletInputStream extends ServletInputStream
    {

        private ByteArrayInputStream cachedStream;

        public CachedServletInputStream(ServletInputStream servletInputStream)
        {
            try
            {
                byte[] bytes = StreamUtils.getBytes(servletInputStream);
                this.cachedStream = new ByteArrayInputStream(bytes);
            }
            catch (IOException e)
            {
            }
        }

        @Override
        public int available() throws IOException
        {
            return this.cachedStream.available();
        }

        @Override
        public void close() throws IOException
        {
            this.cachedStream.close();
        }

        @Override
        protected void finalize() throws Throwable
        {
        }

        @Override
        public synchronized void mark(int readlimit)
        {
            this.cachedStream.mark(readlimit);
        }

        @Override
        public boolean markSupported()
        {
            return true;
        }

        @Override
        public int read() throws IOException
        {
            return this.cachedStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            return this.cachedStream.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException
        {
            return this.cachedStream.read(b);
        }

        @Override
        public synchronized void reset() throws IOException
        {
            this.cachedStream.reset();
        }

        @Override
        public long skip(long n) throws IOException
        {
            return this.cachedStream.skip(n);
        }
    }
}
