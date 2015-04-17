/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import org.mule.module.http.internal.multipart.HttpPart;

import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.PartSource;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.Part;

public class PartWrapper extends FilePart {

    public PartWrapper(Part part)
    {
        super(part.getName(), new HttpPartSource(part), part.getContentType(), null, null);
    }

    private static class HttpPartSource implements PartSource
    {
        private Part part;

        public HttpPartSource(Part part)
        {
            this.part = part;
        }

        @Override
        public long getLength()
        {
            return part.getSize();
        }

        @Override
        public String getFileName()
        {
            if (part instanceof HttpPart)
            {
                return ((HttpPart) part).getFileName();
            }
            return part.getName();
        }

        @Override
        public InputStream createInputStream() throws IOException
        {
            return part.getInputStream();
        }
    }
}
