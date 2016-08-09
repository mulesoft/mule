/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_PARAMS_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.transformer.EndpointAwareTransformer;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.PatchMethod;
import org.mule.compatibility.transport.http.StreamPayloadRequestEntity;
import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.compatibility.transport.http.multipart.MultiPartInputStream;
import org.mule.compatibility.transport.http.multipart.PartDataSource;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.message.ds.StringDataSource;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.EncodingUtil;

/**
 * <code>ObjectToHttpClientMethodRequest</code> transforms a MuleMessage into a HttpClient HttpMethod that represents an
 * HttpRequest.
 */
public class ObjectToHttpClientMethodRequest extends AbstractMessageTransformer implements EndpointAwareTransformer {

  public static final String CHARSET_PARAM_NAME = "charset";

  public ObjectToHttpClientMethodRequest() {
    setReturnDataType(DataType.fromType(HttpMethod.class));
    registerSourceType(DataType.MULE_MESSAGE);
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.fromType(OutputHandler.class));
    registerSourceType(DataType.fromType(Map.class));
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    final MuleMessage msg = event.getMessage();
    String method = detectHttpMethod(msg);
    try {
      HttpMethod httpMethod;

      if (HttpConstants.METHOD_GET.equals(method)) {
        httpMethod = createGetMethod(msg, outputEncoding);
      } else if (HttpConstants.METHOD_POST.equalsIgnoreCase(method)) {
        httpMethod = createPostMethod(event, outputEncoding);
      } else if (HttpConstants.METHOD_PUT.equalsIgnoreCase(method)) {
        httpMethod = createPutMethod(event, outputEncoding);
      } else if (HttpConstants.METHOD_DELETE.equalsIgnoreCase(method)) {
        httpMethod = createDeleteMethod(msg);
      } else if (HttpConstants.METHOD_HEAD.equalsIgnoreCase(method)) {
        httpMethod = createHeadMethod(msg);
      } else if (HttpConstants.METHOD_OPTIONS.equalsIgnoreCase(method)) {
        httpMethod = createOptionsMethod(msg);
      } else if (HttpConstants.METHOD_TRACE.equalsIgnoreCase(method)) {
        httpMethod = createTraceMethod(msg);
      } else if (HttpConstants.METHOD_PATCH.equalsIgnoreCase(method)) {
        httpMethod = createPatchMethod(event, outputEncoding);
      } else {
        throw new TransformerException(HttpMessages.unsupportedMethod(method));
      }

      // Allow the user to set HttpMethodParams as an object on the message
      final HttpMethodParams params = msg.getOutboundProperty(HTTP_PARAMS_PROPERTY);
      event.setMessage(MuleMessage.builder(event.getMessage()).removeOutboundProperty(HTTP_PARAMS_PROPERTY).build());

      if (params != null) {
        httpMethod.setParams(params);
      } else {
        // TODO we should probably set other properties here
        final String httpVersion = msg.getOutboundProperty(HttpConnector.HTTP_VERSION_PROPERTY, HttpConstants.HTTP11);
        if (HttpConstants.HTTP10.equals(httpVersion)) {
          httpMethod.getParams().setVersion(HttpVersion.HTTP_1_0);
        } else {
          httpMethod.getParams().setVersion(HttpVersion.HTTP_1_1);
        }
      }

      setHeaders(httpMethod, msg);

      return httpMethod;
    } catch (final Exception e) {
      throw new TransformerException(this, e);
    }
  }

  protected String detectHttpMethod(MuleMessage msg) {
    return msg.getOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST);
  }

  protected HttpMethod createGetMethod(MuleMessage msg, Charset outputEncoding) throws Exception {
    final Object src = msg.getPayload();
    // TODO It makes testing much harder if we use the endpoint on the
    // transformer since we need to create correct message types and endpoints
    // URI uri = getEndpoint().getEndpointURI().getUri();
    final URI uri = getURI(msg);
    HttpMethod httpMethod;
    String query = uri.getRawQuery();

    httpMethod = new GetMethod(uri.toString());
    String paramName = msg.getOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY, null);
    if (paramName != null) {
      paramName = URLEncoder.encode(paramName, outputEncoding.name());

      String paramValue;
      if (msg.getOutboundProperty(HttpConnector.HTTP_ENCODE_PARAMVALUE, true)) {
        paramValue = URLEncoder.encode(src.toString(), outputEncoding.name());
      } else {
        paramValue = src.toString();
      }

      if (src != null && !StringUtils.EMPTY.equals(src)) {
        if (query == null) {
          query = paramName + "=" + paramValue;
        } else {
          query += "&" + paramName + "=" + paramValue;
        }
      }
    }

    httpMethod.setQueryString(query);
    return httpMethod;
  }

  protected HttpMethod createPostMethod(MuleEvent event, Charset outputEncoding) throws Exception {
    final MuleMessage msg = event.getMessage();
    URI uri = getURI(msg);
    PostMethod postMethod = new PostMethod(uri.toString());

    String bodyParameterName = getBodyParameterName(msg);
    Object src = msg.getPayload();
    if (src instanceof Map) {
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) src).entrySet()) {
        postMethod.addParameter(entry.getKey().toString(), entry.getValue().toString());
      }
    } else if (bodyParameterName != null) {
      postMethod.addParameter(bodyParameterName, src.toString());

    } else {
      setupEntityMethod(src, outputEncoding, event, postMethod);
    }
    checkForContentType(msg, postMethod);

    return postMethod;
  }

  private void checkForContentType(MuleMessage msg, EntityEnclosingMethod method) {
    // TODO MULE-9986 need MuleMessage to support multipart payload
    if (!msg.getInboundPropertyNames().contains("multipart_" + HEADER_CONTENT_TYPE)) {
      // if a content type was specified on the endpoint, use it
      final MediaType mediaType = msg.getDataType().getMediaType();
      if (!MediaType.ANY.matches(mediaType)) {
        method.setRequestHeader(HEADER_CONTENT_TYPE, mediaType.toRfcString());
      }
    }
  }

  protected String getBodyParameterName(MuleMessage message) {
    return message.getOutboundProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY);
  }

  protected HttpMethod createPutMethod(MuleEvent event, Charset outputEncoding) throws Exception {
    final MuleMessage msg = event.getMessage();
    URI uri = getURI(msg);
    PutMethod putMethod = new PutMethod(uri.toString());

    Object payload = msg.getPayload();
    setupEntityMethod(payload, outputEncoding, event, putMethod);
    checkForContentType(msg, putMethod);
    return putMethod;
  }

  protected HttpMethod createDeleteMethod(MuleMessage message) throws Exception {
    URI uri = getURI(message);
    return new DeleteMethod(uri.toString());
  }

  protected HttpMethod createHeadMethod(MuleMessage message) throws Exception {
    URI uri = getURI(message);
    return new HeadMethod(uri.toString());
  }

  protected HttpMethod createOptionsMethod(MuleMessage message) throws Exception {
    URI uri = getURI(message);
    return new OptionsMethod(uri.toString());
  }

  protected HttpMethod createTraceMethod(MuleMessage message) throws Exception {
    URI uri = getURI(message);
    return new TraceMethod(uri.toString());
  }

  protected HttpMethod createPatchMethod(MuleEvent event, Charset outputEncoding) throws Exception {
    final MuleMessage message = event.getMessage();
    URI uri = getURI(message);
    PatchMethod patchMethod = new PatchMethod(uri.toString());

    Object payload = message.getPayload();
    setupEntityMethod(payload, outputEncoding, event, patchMethod);
    checkForContentType(message, patchMethod);
    return patchMethod;
  }

  protected URI getURI(MuleMessage message) throws URISyntaxException, TransformerException {
    String endpointAddress = message.getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, null);
    if (endpointAddress == null) {
      throw new TransformerException(HttpMessages.eventPropertyNotSetCannotProcessRequest(MuleProperties.MULE_ENDPOINT_PROPERTY),
                                     this);
    }
    return new URI(endpointAddress);
  }

  protected void setupEntityMethod(Object src, Charset encoding, MuleEvent event, EntityEnclosingMethod postMethod)
      throws UnsupportedEncodingException, TransformerException {
    final MuleMessage msg = event.getMessage();
    // Dont set a POST payload if the body is a Null Payload.
    // This way client calls can control if a POST body is posted explicitly
    if (msg.getPayload() != null) {
      String outboundMimeType = msg.getDataType().getMediaType().toRfcString();
      if (outboundMimeType == null) {
        outboundMimeType =
            (getEndpoint() != null && getEndpoint().getMimeType() != null ? getEndpoint().getMimeType().toRfcString() : null);
      }
      if (outboundMimeType == null) {
        if (!msg.getDataType().getMediaType().equals(MediaType.ANY)) {
          outboundMimeType = msg.getDataType().getMediaType().toRfcString();
        } else {
          outboundMimeType = HttpConstants.DEFAULT_CONTENT_TYPE;
          if (logger.isDebugEnabled()) {
            logger.debug("Content-Type not set on outgoing request, defaulting to: " + outboundMimeType);
          }
        }
      }

      if (encoding != null && !UTF_8.equals(encoding) && outboundMimeType.indexOf(CHARSET_PARAM_NAME) == -1) {
        outboundMimeType += "; charset=" + encoding.name();
      }

      // Ensure that we have a cached representation of the message if we're
      // using HTTP 1.0
      final String httpVersion = msg.getOutboundProperty(HttpConnector.HTTP_VERSION_PROPERTY, HttpConstants.HTTP11);
      if (HttpConstants.HTTP10.equals(httpVersion)) {
        try {
          src = event.getMessageAsBytes();
        } catch (final Exception e) {
          throw new TransformerException(this, e);
        }
      }

      if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0) {
        try {
          postMethod.setRequestEntity(createMultiPart(event, postMethod));
          return;
        } catch (final Exception e) {
          throw new TransformerException(this, e);
        }
      }
      if (src instanceof String) {
        postMethod.setRequestEntity(new StringRequestEntity(src.toString(), outboundMimeType, encoding.name()));
        return;
      }

      if (src instanceof InputStream) {
        postMethod.setRequestEntity(new InputStreamRequestEntity((InputStream) src, outboundMimeType));
      } else if (src instanceof byte[]) {
        postMethod.setRequestEntity(new ByteArrayRequestEntity((byte[]) src, outboundMimeType));
      } else if (src instanceof OutputHandler) {
        final MuleEvent eventFromContext = RequestContext.getEvent();
        postMethod.setRequestEntity(new StreamPayloadRequestEntity((OutputHandler) src, eventFromContext));
      } else {
        final byte[] buffer = muleContext.getObjectSerializer().serialize(src);
        postMethod.setRequestEntity(new ByteArrayRequestEntity(buffer, outboundMimeType));
      }
    } else if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0) {
      try {
        postMethod.setRequestEntity(createMultiPart(event, postMethod));
      } catch (Exception e) {
        throw new TransformerException(this, e);
      }
    }
  }

  protected void setHeaders(HttpMethod httpMethod, MuleMessage msg) throws TransformerException {
    for (String headerName : msg.getOutboundPropertyNames()) {
      String headerValue = ObjectUtils.getString(msg.getOutboundProperty(headerName), null);

      if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX)) {
        // Define Mule headers a custom headers
        headerName = new StringBuilder(30).append("X-").append(headerName).toString();
        httpMethod.addRequestHeader(headerName, headerValue);
      }

      else if (!HttpConstants.RESPONSE_HEADER_NAMES.containsKey(headerName)
          && !HttpConstants.HEADER_CONTENT_TYPE.contains(headerName)
          && !HttpConnector.HTTP_INBOUND_PROPERTIES.contains(headerName)
          && !HttpConnector.HTTP_COOKIES_PROPERTY.equals(headerName)) {
        httpMethod.addRequestHeader(headerName, headerValue);
      }
    }
  }

  protected MultipartRequestEntity createMultiPart(MuleEvent event, EntityEnclosingMethod method) throws Exception {
    final MuleMessage msg = event.getMessage();
    Part[] parts;
    int i = 0;
    if (msg.getPayload() == null) {
      parts = new Part[msg.getOutboundAttachmentNames().size()];
    } else {
      parts = new Part[msg.getOutboundAttachmentNames().size() + 1];
      parts[i++] = new FilePart("payload", new ByteArrayPartSource("payload", event.getMessageAsBytes()));
    }

    for (final Iterator<String> iterator = msg.getOutboundAttachmentNames().iterator(); iterator.hasNext(); i++) {
      final String attachmentName = iterator.next();
      final DataHandler dh = msg.getOutboundAttachment(attachmentName);
      String fileName = dh.getName();
      if (dh.getDataSource() instanceof StringDataSource) {
        final StringDataSource ds = (StringDataSource) dh.getDataSource();
        final MediaType mediaType = MediaType.parse(ds.getContentType());
        final String charset = mediaType.getCharset().isPresent() ? mediaType.getCharset().get().name() : null;
        final String contentType = mediaType.getPrimaryType() + "/" + mediaType.getSubType();
        parts[i] = new CustomStringPart(ds.getName(), IOUtils.toString(ds.getInputStream()), charset, contentType);
      } else {
        if (dh.getDataSource() instanceof FileDataSource) {
          fileName = ((FileDataSource) dh.getDataSource()).getFile().getName();
        } else if (dh.getDataSource() instanceof URLDataSource) {
          fileName = ((URLDataSource) dh.getDataSource()).getURL().getFile();
          // Don't use the whole file path, just the file name
          final int x = fileName.lastIndexOf("/");
          if (x > -1) {
            fileName = fileName.substring(x + 1);
          }
        } else if (dh.getDataSource() instanceof PartDataSource) {
          org.mule.compatibility.transport.http.multipart.Part part = ((PartDataSource) dh.getDataSource()).getPart();
          if (part instanceof MultiPartInputStream.MultiPart) {
            String partFileName = ((MultiPartInputStream.MultiPart) part).getContentDispositionFilename();
            if (!StringUtils.isEmpty(partFileName)) {
              fileName = partFileName;
            }
          }
        }

        parts[i] = new FilePart(
                                attachmentName, new ByteArrayPartSource(StringUtils.defaultString(fileName, attachmentName),
                                                                        IOUtils.toByteArray(dh.getInputStream())),
                                dh.getContentType(), null);
      }
    }

    return new MultipartRequestEntity(parts, method.getParams());
  }

  /**
   * The endpoint that this transformer instance is configured on
   */
  protected ImmutableEndpoint endpoint = null;

  @Override
  public ImmutableEndpoint getEndpoint() {
    return endpoint;
  }

  @Override
  public void setEndpoint(ImmutableEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  /**
   * Replaces {@link org.apache.commons.httpclient.methods.multipart.StringPart} which does not let us change the content type of
   * the part.
   */
  private static class CustomStringPart extends PartBase {

    public static final String DEFAULT_CONTENT_TYPE = "text/plain";
    public static final String DEFAULT_CHARSET = "US-ASCII";
    public static final String DEFAULT_TRANSFER_ENCODING = "8bit";

    private byte[] content;
    private String value;

    public CustomStringPart(String name, String value, String charset, String contentType) {
      super(name, contentType, charset == null ? DEFAULT_CHARSET : charset, DEFAULT_TRANSFER_ENCODING);
      if (value == null) {
        throw new IllegalArgumentException("Value may not be null");
      } else if (value.indexOf(0) != -1) {
        throw new IllegalArgumentException("NULs may not be present in string parts");
      } else {
        this.value = value;
      }
    }

    private byte[] getContent() {
      if (this.content == null) {
        this.content = EncodingUtil.getBytes(this.value, this.getCharSet());
      }

      return this.content;
    }

    protected void sendData(OutputStream out) throws IOException {
      out.write(this.getContent());
    }

    protected long lengthOfData() throws IOException {
      return (long) this.getContent().length;
    }

    public void setCharSet(String charSet) {
      super.setCharSet(charSet);
      this.content = null;
    }
  }
}
