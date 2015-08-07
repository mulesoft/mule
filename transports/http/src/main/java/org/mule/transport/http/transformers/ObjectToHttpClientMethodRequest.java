/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.message.ds.StringDataSource;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.PatchMethod;
import org.mule.transport.http.StreamPayloadRequestEntity;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.transport.http.multipart.MultiPartInputStream;
import org.mule.transport.http.multipart.PartDataSource;
import org.mule.util.IOUtils;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 * <code>ObjectToHttpClientMethodRequest</code> transforms a MuleMessage into a
 * HttpClient HttpMethod that represents an HttpRequest.
 */
public class ObjectToHttpClientMethodRequest extends AbstractMessageTransformer
{
    public ObjectToHttpClientMethodRequest()
    {
        setReturnDataType(DataTypeFactory.create(HttpMethod.class));
        registerSourceType(DataTypeFactory.MULE_MESSAGE);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(OutputHandler.class));
        registerSourceType(DataTypeFactory.create(NullPayload.class));
        registerSourceType(DataTypeFactory.create(Map.class));
    }

    @Override
    public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
    {
        String method = detectHttpMethod(msg);
        try
        {
            HttpMethod httpMethod;

            if (HttpConstants.METHOD_GET.equals(method))
            {
                httpMethod = createGetMethod(msg, outputEncoding);
            }
            else if (HttpConstants.METHOD_POST.equalsIgnoreCase(method))
            {
                httpMethod = createPostMethod(msg, outputEncoding);
            }
            else if (HttpConstants.METHOD_PUT.equalsIgnoreCase(method))
            {
                httpMethod = createPutMethod(msg, outputEncoding);
            }
            else if (HttpConstants.METHOD_DELETE.equalsIgnoreCase(method))
            {
                httpMethod = createDeleteMethod(msg);
            }
            else if (HttpConstants.METHOD_HEAD.equalsIgnoreCase(method))
            {
                httpMethod = createHeadMethod(msg);
            }
            else if (HttpConstants.METHOD_OPTIONS.equalsIgnoreCase(method))
            {
                httpMethod = createOptionsMethod(msg);
            }
            else if (HttpConstants.METHOD_TRACE.equalsIgnoreCase(method))
            {
                httpMethod = createTraceMethod(msg);
            }
            else if (HttpConstants.METHOD_PATCH.equalsIgnoreCase(method))
            {
                httpMethod = createPatchMethod(msg, outputEncoding);
            }
            else
            {
                throw new TransformerException(HttpMessages.unsupportedMethod(method));
            }

            // Allow the user to set HttpMethodParams as an object on the message
            final HttpMethodParams params = (HttpMethodParams) msg.removeProperty(
                HttpConnector.HTTP_PARAMS_PROPERTY, PropertyScope.OUTBOUND);
            if (params != null)
            {
                httpMethod.setParams(params);
            }
            else
            {
                // TODO we should probably set other properties here
                final String httpVersion = msg.getOutboundProperty(HttpConnector.HTTP_VERSION_PROPERTY,
                    HttpConstants.HTTP11);
                if (HttpConstants.HTTP10.equals(httpVersion))
                {
                    httpMethod.getParams().setVersion(HttpVersion.HTTP_1_0);
                }
                else
                {
                    httpMethod.getParams().setVersion(HttpVersion.HTTP_1_1);
                }
            }

            setHeaders(httpMethod, msg);

            return httpMethod;
        }
        catch (final Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected String detectHttpMethod(MuleMessage msg)
    {
        String method = msg.getOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, null);
        if (method == null)
        {
            method = msg.getInvocationProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
        }
        return method;
    }

    protected HttpMethod createGetMethod(MuleMessage msg, String outputEncoding) throws Exception
    {
        final Object src = msg.getPayload();
        // TODO It makes testing much harder if we use the endpoint on the
        // transformer since we need to create correct message types and endpoints
        // URI uri = getEndpoint().getEndpointURI().getUri();
        final URI uri = getURI(msg);
        HttpMethod httpMethod;
        String query = uri.getRawQuery();

        httpMethod = new GetMethod(uri.toString());
        String paramName = msg.getOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY, null);
        if (paramName != null)
        {
            paramName = URLEncoder.encode(paramName, outputEncoding);

            String paramValue;
            Boolean encode = msg.getInvocationProperty(HttpConnector.HTTP_ENCODE_PARAMVALUE);
            if (encode == null)
            {
                encode = msg.getOutboundProperty(HttpConnector.HTTP_ENCODE_PARAMVALUE, true);
            }

            if (encode)
            {
                paramValue = URLEncoder.encode(src.toString(), outputEncoding);
            }
            else
            {
                paramValue = src.toString();
            }

            if (!(src instanceof NullPayload) && !StringUtils.EMPTY.equals(src))
            {
                if (query == null)
                {
                    query = paramName + "=" + paramValue;
                }
                else
                {
                    query += "&" + paramName + "=" + paramValue;
                }
            }
        }

        httpMethod.setQueryString(query);
        return httpMethod;
    }

    protected HttpMethod createPostMethod(MuleMessage msg, String outputEncoding) throws Exception
    {
        URI uri = getURI(msg);
        PostMethod postMethod = new PostMethod(uri.toString());

        String bodyParameterName = getBodyParameterName(msg);
        Object src = msg.getPayload();
        if (src instanceof Map)
        {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) src).entrySet())
            {
                postMethod.addParameter(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        else if (bodyParameterName != null)
        {
            postMethod.addParameter(bodyParameterName, src.toString());

        }
        else
        {
            setupEntityMethod(src, outputEncoding, msg, postMethod);
        }
        checkForContentType(msg, postMethod);

        return postMethod;
    }

    private void checkForContentType(MuleMessage msg, EntityEnclosingMethod method)
    {
        // if a content type was specified on the endpoint, use it
        String outgoingContentType = msg.getInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE);
        if (outgoingContentType != null)
        {
            method.setRequestHeader(HttpConstants.HEADER_CONTENT_TYPE, outgoingContentType);
        }
    }

    protected String getBodyParameterName(MuleMessage message)
    {
        String bodyParameter = message.getOutboundProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY);
        if (bodyParameter == null)
        {
            bodyParameter = message.getInvocationProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY);
        }
        return bodyParameter;
    }

    protected HttpMethod createPutMethod(MuleMessage msg, String outputEncoding) throws Exception
    {
        URI uri = getURI(msg);
        PutMethod putMethod = new PutMethod(uri.toString());

        Object payload = msg.getPayload();
        setupEntityMethod(payload, outputEncoding, msg, putMethod);
        checkForContentType(msg, putMethod);
        return putMethod;
    }

    protected HttpMethod createDeleteMethod(MuleMessage message) throws Exception
    {
        URI uri = getURI(message);
        return new DeleteMethod(uri.toString());
    }

    protected HttpMethod createHeadMethod(MuleMessage message) throws Exception
    {
        URI uri = getURI(message);
        return new HeadMethod(uri.toString());
    }

    protected HttpMethod createOptionsMethod(MuleMessage message) throws Exception
    {
        URI uri = getURI(message);
        return new OptionsMethod(uri.toString());
    }

    protected HttpMethod createTraceMethod(MuleMessage message) throws Exception
    {
        URI uri = getURI(message);
        return new TraceMethod(uri.toString());
    }

    protected HttpMethod createPatchMethod(MuleMessage message, String outputEncoding) throws Exception
    {
        URI uri = getURI(message);
        PatchMethod patchMethod = new PatchMethod(uri.toString());

        Object payload = message.getPayload();
        setupEntityMethod(payload, outputEncoding, message, patchMethod);
        checkForContentType(message, patchMethod);
        return patchMethod;
    }

    protected URI getURI(MuleMessage message) throws URISyntaxException, TransformerException
    {
        String endpointAddress = message.getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
        if (endpointAddress == null)
        {
            throw new TransformerException(
                HttpMessages.eventPropertyNotSetCannotProcessRequest(MuleProperties.MULE_ENDPOINT_PROPERTY),
                this);
        }
        return new URI(endpointAddress);
    }

    protected void setupEntityMethod(Object src,
                                     String encoding,
                                     MuleMessage msg,
                                     EntityEnclosingMethod postMethod)
        throws UnsupportedEncodingException, TransformerException
    {
        // Dont set a POST payload if the body is a Null Payload.
        // This way client calls can control if a POST body is posted explicitly
        if (!(msg.getPayload() instanceof NullPayload))
        {
            String outboundMimeType = (String) msg.getProperty(HttpConstants.HEADER_CONTENT_TYPE,
                PropertyScope.OUTBOUND);
            if (outboundMimeType == null)
            {
                outboundMimeType = (getEndpoint() != null ? getEndpoint().getMimeType() : null);
            }
            if (outboundMimeType == null)
            {
                if (!msg.getDataType().getMimeType().equals(MimeTypes.ANY))
                {
                    outboundMimeType = msg.getDataType().getMimeType();
                }
                else
                {
                    outboundMimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Content-Type not set on outgoing request, defaulting to: " + outboundMimeType);
                    }
                }
            }

            if (encoding != null && !"UTF-8".equals(encoding.toUpperCase())
                && outboundMimeType.indexOf("charset") == -1)
            {
                outboundMimeType += "; charset=" + encoding;
            }

            // Ensure that we have a cached representation of the message if we're
            // using HTTP 1.0
            final String httpVersion = msg.getOutboundProperty(HttpConnector.HTTP_VERSION_PROPERTY,
                HttpConstants.HTTP11);
            if (HttpConstants.HTTP10.equals(httpVersion))
            {
                try
                {
                    if (msg instanceof MuleMessageCollection)
                    {
                        src = msg.getPayload(DataType.BYTE_ARRAY_DATA_TYPE);
                    }
                    else
                    {
                        src = msg.getPayloadAsBytes();
                    }
                }
                catch (final Exception e)
                {
                    throw new TransformerException(this, e);
                }
            }

            if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0)
            {
                try
                {
                    postMethod.setRequestEntity(createMultiPart(msg, postMethod));
                    return;
                }
                catch (final Exception e)
                {
                    throw new TransformerException(this, e);
                }
            }
            if (src instanceof String)
            {
                postMethod.setRequestEntity(new StringRequestEntity(src.toString(), outboundMimeType,
                    encoding));
                return;
            }

            if (src instanceof InputStream)
            {
                postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream) src, outboundMimeType));
            }
            else if (src instanceof byte[])
            {
                postMethod.setRequestEntity(new ByteArrayRequestEntity((byte[]) src, outboundMimeType));
            }
            else if (src instanceof OutputHandler)
            {
                final MuleEvent event = RequestContext.getEvent();
                postMethod.setRequestEntity(new StreamPayloadRequestEntity((OutputHandler) src, event));
            }
            else
            {
                final byte[] buffer = muleContext.getObjectSerializer().serialize(src);
                postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, outboundMimeType));
            }
        }
        else if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0)
        {
            try
            {
                postMethod.setRequestEntity(createMultiPart(msg, postMethod));
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
        }
    }

    protected void setHeaders(HttpMethod httpMethod, MuleMessage msg) throws TransformerException
    {
        for (String headerName : msg.getOutboundPropertyNames())
        {
            String headerValue = ObjectUtils.getString(msg.getOutboundProperty(headerName), null);

            if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX))
            {
                // Define Mule headers a custom headers
                headerName = new StringBuilder(30).append("X-").append(headerName).toString();
                httpMethod.addRequestHeader(headerName, headerValue);

            }

            else if (!HttpConstants.RESPONSE_HEADER_NAMES.containsKey(headerName)
                     && !HttpConnector.HTTP_INBOUND_PROPERTIES.contains(headerName)
                     && !HttpConnector.HTTP_COOKIES_PROPERTY.equals(headerName))
            {

                httpMethod.addRequestHeader(headerName, headerValue);
            }
        }
    }

    protected MultipartRequestEntity createMultiPart(MuleMessage msg, EntityEnclosingMethod method)
        throws Exception
    {
        Part[] parts;
        int i = 0;
        if (msg.getPayload() instanceof NullPayload)
        {
            parts = new Part[msg.getOutboundAttachmentNames().size()];
        }
        else
        {
            parts = new Part[msg.getOutboundAttachmentNames().size() + 1];
            parts[i++] = new FilePart("payload", new ByteArrayPartSource("payload", msg.getPayloadAsBytes()));
        }

        for (final Iterator<String> iterator = msg.getOutboundAttachmentNames().iterator(); iterator.hasNext(); i++)
        {
            final String attachmentName = iterator.next();
            final DataHandler dh = msg.getOutboundAttachment(attachmentName);
            String fileName = dh.getName();
            if (dh.getDataSource() instanceof StringDataSource)
            {
                final StringDataSource ds = (StringDataSource) dh.getDataSource();
                parts[i] = new StringPart(ds.getName(), IOUtils.toString(ds.getInputStream()));
            }
            else
            {
                if (dh.getDataSource() instanceof FileDataSource)
                {
                    fileName = ((FileDataSource) dh.getDataSource()).getFile().getName();
                }
                else if (dh.getDataSource() instanceof URLDataSource)
                {
                    fileName = ((URLDataSource) dh.getDataSource()).getURL().getFile();
                    // Don't use the whole file path, just the file name
                    final int x = fileName.lastIndexOf("/");
                    if (x > -1)
                    {
                        fileName = fileName.substring(x + 1);
                    }
                }
                else if (dh.getDataSource() instanceof PartDataSource)
                {
                    org.mule.transport.http.multipart.Part part = ((PartDataSource) dh.getDataSource()).getPart();
                    if (part instanceof MultiPartInputStream.MultiPart)
                    {
                        String partFileName = ((MultiPartInputStream.MultiPart) part).getContentDispositionFilename();
                        if (!StringUtils.isEmpty(partFileName))
                        {
                            fileName = partFileName;
                        }
                    }
                }

                parts[i] = new FilePart(attachmentName, new ByteArrayPartSource(StringUtils.defaultString(fileName, attachmentName),
                    IOUtils.toByteArray(dh.getInputStream())), dh.getContentType(), null);
            }
        }

        return new MultipartRequestEntity(parts, method.getParams());
    }
}
