/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import static org.mule.util.StringUtils.WHITE_SPACE;
import org.mule.api.MuleRuntimeException;
import org.mule.module.http.internal.multipart.HttpPart;
import org.mule.util.StringUtils;

import com.google.common.base.Charsets;
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

import org.apache.commons.io.IOUtils;

public class HttpParser
{

    private static final String SPACE_ENTITY = "%20";
    private static final String PLUS_SIGN = "\\+";
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

    public static Collection<HttpPart> parseMultipartContent(InputStream content, String contentType) throws IOException
    {
        MimeMultipart mimeMultipart = null;
        List<HttpPart> parts = Lists.newArrayList();

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

                String filename = part.getFileName();
                String partName = filename;
                String[] contentDispositions = part.getHeader(CONTENT_DISPOSITION_PART_HEADER);
                if (contentDispositions != null)
                {
                    String contentDisposition = contentDispositions[0];
                    if (contentDisposition.contains(NAME_ATTRIBUTE))
                    {
                        partName = contentDisposition.substring(contentDisposition.indexOf(NAME_ATTRIBUTE) + NAME_ATTRIBUTE.length() + 2);
                        partName = partName.substring(0, partName.indexOf("\""));
                    }
                }
                HttpPart httpPart = new HttpPart(partName, filename, IOUtils.toByteArray(part.getInputStream()), part.getContentType(), part.getSize());

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
        return decodeUrlEncodedBody(queryString, Charsets.UTF_8.name());
    }

    public static String encodeQueryString(Map parameters)
    {
        return encodeString(Charsets.UTF_8.name(), parameters);
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

                if (idx != -1)
                {
                    addParam(queryParams, pair.substring(0, idx), pair.substring(idx + 1), encoding);
                }
                else
                {
                    addParam(queryParams, pair, null, encoding);

                }
            }
        }
        return queryParams;
    }

    private static void addParam(ParameterMap queryParams, String name, String value, String encoding)
    {
        queryParams.put(decode(name, encoding), decode(value, encoding));
    }


    public static String encodeString(String encoding, Map parameters)
    {
        String body;
        StringBuilder result = new StringBuilder();
        for (Map.Entry<?,?> entry : (Set<Map.Entry<?,?>>)((parameters).entrySet()))
        {
            String paramName = entry.getKey().toString();
            Object paramValue = entry.getValue();

            Iterable paramValues = paramValue instanceof Iterable ? (Iterable) paramValue : Arrays.asList(paramValue);
            for (Object value : paramValues)
            {
                try
                {
                    paramName = URLEncoder.encode(paramName, encoding);
                    paramValue = value != null ? URLEncoder.encode(value.toString(), encoding) : null;
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
                if (paramValue != null)
                {
                    // Allowing parameters name with no value assigned
                    result.append("=");
                    result.append(paramValue);
                }
            }
        }

        body = result.toString();
        return body;
    }

    /**
     * Decodes uri params from a request path
     *
     * @param pathWithUriParams path with uri param place holders
     * @param requestPath request path
     * @return a map with the uri params present in the request path with the values decoded.
     */
    public static ParameterMap decodeUriParams(String pathWithUriParams, String requestPath)
    {
        ParameterMap uriParams = new ParameterMap();
        if (pathWithUriParams.contains("{"))
        {
            final String[] requestPathParts = requestPath.split("/");
            final String[] listenerPathParts = pathWithUriParams.split("/");
            int longerPathSize = Math.min(requestPathParts.length, listenerPathParts.length);
            //split will return an empty string as first path before /
            for (int i = 1; i < longerPathSize; i++)
            {
                final String listenerPart = listenerPathParts[i];
                if (listenerPart.startsWith("{") && listenerPart.endsWith("}"))
                {
                    String parameterName = listenerPart.substring(1, listenerPart.length() - 1);
                    String parameterValue = requestPathParts[i];
                    uriParams.put(parameterName, decode(parameterValue, Charsets.UTF_8.displayName()));
                }
            }
        }
        return uriParams;
    }

    private static String decode(String text, String encoding)
    {
        if(text == null)
        {
            return null;
        }
        try
        {
            return URLDecoder.decode(text, encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new MuleRuntimeException(e);
        }
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

    /**
     * Normalize a path that may contains spaces, %20 or +.
     *
     * @param path path with encoded spaces or raw spaces
     * @return path with only spaces.
     */
    public static String normalizePathWithSpacesOrEncodedSpaces(String path)
    {
        return path.replaceAll(SPACE_ENTITY, WHITE_SPACE).replaceAll(PLUS_SIGN, WHITE_SPACE);
    }

    /**
     * Encodes spaces in a path, replacing them by %20.
     *
     * @param path Path that may contain spaces
     * @return The path with all spaces replaced by %20.
     */
    public static String encodeSpaces(String path)
    {
        return path.replaceAll(WHITE_SPACE, SPACE_ENTITY);
    }

    /**
     * Appends a query parameter to an URL that may or may not contain query parameters already.
     *
     * @param url base URL to apply the new query parameter
     * @param queryParamName query parameter name
     * @param queryParamValue query parameter value
     * @return a new string with the query parameter appended
     */
    public static String appendQueryParam(String url, String queryParamName, String queryParamValue)
    {
        try
        {
            String urlPreparedForNewParameter = url.contains("?") ? url + "&" : url + "?";
            return urlPreparedForNewParameter + URLEncoder.encode(queryParamName, Charsets.UTF_8.name()) + "=" + URLEncoder.encode(queryParamValue, Charsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
