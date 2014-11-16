/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import org.mule.api.MuleRuntimeException;
import org.mule.module.http.internal.multipart.HttpPart;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.utils.Charsets;

public class HttpParser
{
    private static final String CONTENT_DISPOSITION_PART_HEADER = "Content-Disposition";
    private static final String NAME_ATTRIBUTE = "name";

    public static String extractPath(String uri)
    {
        String path = uri;
        int i = path.indexOf('?');
        if (i > -1)
        {
            path = path.substring(0, i);
        }
        return path;
    }

    public static String extractQueryParams(String uri)
    {
        int i = uri.indexOf("?");
        String queryString = "";
        if(i > -1)
        {
            queryString = uri.substring(i + 1);
        }
        return queryString;
    }

    public static Collection<Part> parseMultipartContent(InputStream content, String contentType) throws IOException
    {
        MimeMultipart mimeMultipart = null;
        List<Part> parts = Lists.newArrayList();

        try
        {
            mimeMultipart = new MimeMultipart(new ByteArrayDataSource(content, contentType));
        }
        catch (MessagingException e)
        {
            throw new IOException(e);
        }

        try
        {
            int partCount = mimeMultipart.getCount();

            for (int i = 0; i < partCount; i++)
            {
                BodyPart part = mimeMultipart.getBodyPart(i);

                String partName = part.getFileName();
                String contentDisposition = part.getHeader(CONTENT_DISPOSITION_PART_HEADER)[0];
                if (contentDisposition.contains(NAME_ATTRIBUTE))
                {
                    partName = contentDisposition.substring(contentDisposition.indexOf(NAME_ATTRIBUTE) + NAME_ATTRIBUTE.length() + 2);
                    partName = partName.substring(0, partName.indexOf("\""));
                }

                HttpPart httpPart = new HttpPart(partName, IOUtils.toByteArray(part.getInputStream()), part.getContentType(), part.getSize());

                Enumeration<Header> headers = part.getAllHeaders();

                while (headers.hasMoreElements())
                {
                    Header header = headers.nextElement();
                    httpPart.addHeader(header.getName(), header.getValue());
                }
                parts.add(httpPart);
            }
        }
        catch (MessagingException e)
        {
            throw new IOException(e);
        }

        return parts;
    }

    public static String sanitizePathWithStartSlash(String path)
    {
        if (path == null)
        {
            return null;
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    public static ParameterMap decodeQueryString(String queryString)
    {
        return decodeUrlEncodedBody(queryString, Charsets.UTF8_CHARSET.name());
    }

    public static String encodeQueryString(Map parameters)
    {
        return encodeString(Charsets.UTF8_CHARSET.name(), parameters);
    }

    public static ParameterMap decodeUrlEncodedBody(String urlEncodedBody, String encoding)
    {
        return decodeString(urlEncodedBody, encoding);
    }

    public static ParameterMap decodeString(String encodedString, String encoding)
    {
        ParameterMap queryParams = new ParameterMap();
        if (!StringUtils.isBlank(encodedString))
        {
            String[] pairs = encodedString.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try
                {
                    queryParams.put(URLDecoder.decode(pair.substring(0, idx), encoding), URLDecoder.decode(pair.substring(idx + 1), encoding));
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
        }
        return queryParams;
    }

    public static String encodeString(String encoding, Map parameters)
    {
        String body;
        StringBuilder result = new StringBuilder();
        for (Map.Entry<?,?> entry : (Set<Map.Entry<?,?>>)((parameters).entrySet()))
        {
            String paramName = (String) entry.getKey();
            Object paramValue = entry.getValue();

            Iterable paramValues = paramValue instanceof Iterable ? (Iterable) paramValue : Arrays.asList(paramValue);
            for (Object value : paramValues)
            {
                try
                {
                    paramName = URLEncoder.encode(paramName, encoding);
                    paramValue = URLEncoder.encode(value.toString(), encoding);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new MuleRuntimeException(e);
                }

                if (result.length() > 0)
                {
                    result.append("&");
                }
                result.append(paramName);
                result.append("=");
                result.append(paramValue);
            }
        }

        body = result.toString();
        return body;
    }

    /**
     * Extracts the subtype from a content type
     *
     * @param contentType the content type
     * @return subtype of the content type.
     */
    public static String getContentTypeSubType(String contentType)
    {
        final ContentType contentTypeValue;
        try
        {
            contentTypeValue = new ContentType(contentType);
            return contentTypeValue.getSubType();
        }
        catch (ParseException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
