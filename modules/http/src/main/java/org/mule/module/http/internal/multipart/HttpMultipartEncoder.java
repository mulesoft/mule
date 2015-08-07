/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.multipart;

import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

/**
 * Creates multipart
 */
public class HttpMultipartEncoder
{

    private static final String FORM_DATA = "form-data";

    public static MimeMultipart createMultpartContent(MultipartHttpEntity body, String contentType)
    {
        String contentTypeSubType = HttpParser.getContentTypeSubType(contentType);
        MimeMultipart mimeMultipartContent = new HttpMimeMultipart(contentType, contentTypeSubType);
        final Collection<HttpPart> parts = body.getParts();

        for (HttpPart part : parts)
        {
            final InternetHeaders internetHeaders = new InternetHeaders();
            for (String headerName : part.getHeaderNames())
            {
                final Collection<String> headerValues = part.getHeaders(headerName);
                for (String headerValue : headerValues)
                {
                    internetHeaders.addHeader(headerName, headerValue);
                }
            }
            if (internetHeaders.getHeader(CONTENT_DISPOSITION) == null && contentTypeSubType.equals(FORM_DATA))
            {
                internetHeaders.addHeader(CONTENT_DISPOSITION, getContentDisposition(part));
            }
            if (internetHeaders.getHeader(CONTENT_TYPE) == null && part.getContentType() != null)
            {
                internetHeaders.addHeader(CONTENT_TYPE, part.getContentType());
            }
            try
            {
                final byte[] partContent = IOUtils.toByteArray(part.getInputStream());
                mimeMultipartContent.addBodyPart(new MimeBodyPart(internetHeaders, partContent));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return mimeMultipartContent;
    }

    private static String getContentDisposition(HttpPart part)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(FORM_DATA);
        builder.append("; name=\"");
        builder.append(part.getName());
        builder.append("\"");
        if (part.getFileName() != null)
        {
            builder.append("; filename=\"");
            builder.append(part.getFileName());
            builder.append("\"");
        }
        return builder.toString();
    }

    public static byte[] createMultipartContent(MultipartHttpEntity multipartEntity, String contentType) throws IOException, MessagingException
    {
        MimeMultipart mimeMultipartContent = HttpMultipartEncoder.createMultpartContent(multipartEntity, contentType);
        final ByteArrayOutputStream byteArrayOutputStream;
        byteArrayOutputStream = new ByteArrayOutputStream();
        mimeMultipartContent.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
