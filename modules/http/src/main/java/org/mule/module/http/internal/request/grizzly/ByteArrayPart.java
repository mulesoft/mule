/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import static java.nio.charset.StandardCharsets.US_ASCII;

import com.ning.http.client.multipart.PartVisitor;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Our version of Async Http Client's class to support setting headers to custom values (see MULE-10233).
 *
 * @since 3.8.3
 */
public class ByteArrayPart extends com.ning.http.client.multipart.ByteArrayPart
{
    protected String customContentDisposition;
    protected String customContentType;

    public ByteArrayPart(String name, byte[] bytes) {
        this(name, bytes, null);
    }

    public ByteArrayPart(String name, byte[] bytes, String contentType) {
        this(name, bytes, contentType, null);
    }

    public ByteArrayPart(String name, byte[] bytes, String contentType, Charset charset) {
        this(name, bytes, contentType, charset, null);
    }

    public ByteArrayPart(String name, byte[] bytes, String contentType, Charset charset, String fileName) {
        this(name, bytes, contentType, charset, fileName, null);
    }

    public ByteArrayPart(String name, byte[] bytes, String contentType, Charset charset, String fileName, String contentId) {
        this(name, bytes, contentType, charset, fileName, contentId, null);
    }

    public ByteArrayPart(String name, byte[] bytes, String contentType, Charset charset, String fileName, String contentId, String transferEncoding) {
        super(name, bytes, contentType, charset, fileName, contentId, transferEncoding);
    }

    @Override
    protected void visitDispositionHeader(PartVisitor visitor) throws IOException
    {
        if (hasCustomContentDisposition())
        {
            visitor.withBytes(CRLF_BYTES);
            visitor.withBytes(CONTENT_DISPOSITION_BYTES);
            visitor.withBytes(customContentDisposition.getBytes(US_ASCII));
        }
        else
        {
            super.visitDispositionHeader(visitor);
        }
    }

    @Override
    protected void visitContentTypeHeader(PartVisitor visitor) throws IOException {
        if (hasCustomContentType())
        {
            visitor.withBytes(CRLF_BYTES);
            visitor.withBytes(CONTENT_TYPE_BYTES);
            visitor.withBytes(customContentType.getBytes(US_ASCII));
        }
        else
        {
            super.visitContentTypeHeader(visitor);
        }
    }

    public boolean hasCustomContentDisposition()
    {
        return customContentDisposition != null;
    }

    public void setCustomContentDisposition(String customContentDisposition)
    {
        this.customContentDisposition = customContentDisposition;
    }

    public boolean hasCustomContentType()
    {
        return customContentType != null;
    }

    public void setCustomContentType(String customContentType)
    {
        this.customContentType = customContentType;
    }
}
