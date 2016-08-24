/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.OutputHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.httpclient.ContentLengthInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.StatusLine;

/**
 * A generic HTTP response wrapper.
 */
public class HttpResponse {

  private HttpVersion ver = HttpVersion.HTTP_1_1;
  private int statusCode = HttpStatus.SC_OK;
  private String phrase = HttpStatus.getStatusText(HttpStatus.SC_OK);
  private HeaderGroup headers = new HeaderGroup();
  private boolean keepAlive = false;
  private boolean disableKeepAlive = false;
  private Charset fallbackCharset = ISO_8859_1;
  private OutputHandler outputHandler;

  public HttpResponse() {
    super();
  }

  public HttpResponse(final StatusLine statusline, final Header[] headers, final InputStream content)
      throws IOException {
    super();
    if (statusline == null) {
      throw new IllegalArgumentException("Status line may not be null");
    }
    setStatusLine(HttpVersion.parse(statusline.getHttpVersion()), statusline.getStatusCode(),
                  statusline.getReasonPhrase());
    setHeaders(headers);
    if (content != null) {
      InputStream in = content;
      Header contentLength = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_LENGTH);
      Header transferEncoding = this.headers.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);

      if (transferEncoding != null) {
        if (transferEncoding.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1) {
          in = new ChunkedInputStream(in);
        }
      } else if (contentLength != null) {
        long len = getContentLength();
        if (len >= 0) {
          in = new ContentLengthInputStream(in, len);
        }
      }
    }
  }

  public void setStatusLine(final HttpVersion ver, int statuscode, final String phrase) {
    if (ver == null) {
      throw new IllegalArgumentException("HTTP version may not be null");
    }
    if (statuscode <= 0) {
      throw new IllegalArgumentException("Status code may not be negative or zero");
    }
    this.ver = ver;
    this.statusCode = statuscode;
    if (phrase != null) {
      this.phrase = phrase;
    } else {
      this.phrase = HttpStatus.getStatusText(statuscode);
    }
  }

  public void setStatusLine(final HttpVersion ver, int statuscode) {
    setStatusLine(ver, statuscode, null);
  }

  public String getPhrase() {
    return this.phrase;
  }

  /**
   * @deprecated use {@link #getStatusCode()} instead
   * @return HTTP status code
   */
  @Deprecated
  public int getStatuscode() {
    return this.getStatusCode();
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public HttpVersion getHttpVersion() {
    return this.ver;
  }

  public String getStatusLine() {
    StringBuilder buffer = new StringBuilder(64);
    buffer.append(this.ver);
    buffer.append(' ');
    buffer.append(this.statusCode);
    if (this.phrase != null) {
      buffer.append(' ');
      buffer.append(this.phrase);
    }
    return buffer.toString();
  }

  public boolean containsHeader(final String name) {
    return this.headers.containsHeader(name);
  }

  public Header[] getHeaders() {
    return this.headers.getAllHeaders();
  }

  public Header getFirstHeader(final String name) {
    return this.headers.getFirstHeader(name);
  }

  public void removeHeaders(final String s) {
    if (s == null) {
      return;
    }
    Header[] headers = this.headers.getHeaders(s);
    for (Header header : headers) {
      this.headers.removeHeader(header);
    }
  }

  public void addHeader(final Header header) {
    if (header == null) {
      return;
    }
    this.headers.addHeader(header);
  }

  public void setHeader(final Header header) {
    if (header == null) {
      return;
    }
    removeHeaders(header.getName());
    addHeader(header);
  }

  public void setHeaders(final Header[] headers) {
    if (headers == null) {
      return;
    }
    this.headers.setHeaders(headers);
  }

  public Iterator getHeaderIterator() {
    return this.headers.getIterator();
  }

  public Charset getCharset() {
    Charset charset = getFallbackCharset();
    Header contenttype = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_TYPE);
    if (contenttype != null) {
      HeaderElement values[] = contenttype.getElements();
      if (values.length == 1) {
        NameValuePair param = values[0].getParameterByName("charset");
        if (param != null) {
          charset = Charset.forName(param.getValue());
        }
      }
    }
    return charset;
  }

  public long getContentLength() {
    Header contentLength = this.headers.getFirstHeader(HttpConstants.HEADER_CONTENT_LENGTH);
    if (contentLength != null) {
      try {
        return Long.parseLong(contentLength.getValue());
      } catch (NumberFormatException e) {
        return -1;
      }
    } else {
      return -1;
    }
  }

  public boolean hasBody() {
    return outputHandler != null;
  }

  public OutputHandler getBody() throws IOException {
    return outputHandler;
  }

  public void setBody(MuleMessage msg, MuleContext muleContext) throws Exception {
    if (msg == null)
      return;

    // TODO MULE-5005 response attachments
    // if(msg.getOutboundAttachmentNames().size() > 0)
    // {
    // setBody(createMultipart());
    // setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, MimeTypes.MULTIPART_MIXED));
    // return;
    // }

    Object payload = msg.getPayload();
    if (payload instanceof String) {
      setBody(payload.toString());
    } else if (payload == null) {
      return;
    } else if (payload instanceof byte[]) {
      setBody((byte[]) payload);
    } else {
      setBody((OutputHandler) muleContext.getTransformationService().transform(msg, DataType.fromType(OutputHandler.class))
          .getPayload());
    }
  }

  public void setBody(OutputHandler outputHandler) {
    this.outputHandler = outputHandler;
  }

  public void setBody(final String string) {
    setBody(string.getBytes(getCharset()));
  }

  private void setBody(final byte[] raw) {
    if (!containsHeader(HttpConstants.HEADER_CONTENT_TYPE)) {
      setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE));
    }
    if (!containsHeader(HttpConstants.HEADER_TRANSFER_ENCODING)) {
      setHeader(new Header(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(raw.length)));
    }

    this.outputHandler = (event, out) -> out.write(raw);
  }

  public String getBodyAsString() throws IOException {
    if (!hasBody())
      return "";

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    outputHandler.write(getCurrentEvent(), out);

    return new String(out.toByteArray(), getCharset());
  }

  public boolean isKeepAlive() {
    return !disableKeepAlive && keepAlive;
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  /**
   * The HTTTP spec suggests that for HTTP 1.1 persistent connections should be used, for HTTP 1.0 the connection should not be
   * kept alive. This method sets up the keepAlive flag according to the <code>version</code> that was passed in.
   */
  protected void setupKeepAliveFromRequestVersion(HttpVersion version) {
    setKeepAlive(version.equals(HttpVersion.HTTP_1_1));
  }

  public void disableKeepAlive(boolean keepalive) {
    disableKeepAlive = keepalive;
  }

  public Charset getFallbackCharset() {
    return fallbackCharset;
  }

  public void setFallbackCharset(Charset overrideCharset) {
    this.fallbackCharset = overrideCharset;
  }

  // TODO MULE-5005 response attachments
  // protected OutputHandler createMultipart() throws Exception
  // {
  //
  // return new OutputHandler() {
  // public void write(MuleEvent event, OutputStream out) throws IOException
  // {
  // MultiPartOutputStream partStream = new MultiPartOutputStream(out, event.getEncoding());
  // try
  // {
  // MuleMessage msg = event.getResult();
  // if (!(msg.getPayload() instanceof NullPayload))
  // {
  // String contentType = msg.getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, MimeTypes.BINARY);
  // partStream.startPart(contentType);
  // try
  // {
  // partStream.getOut().write(msg.getPayloadAsBytes());
  // }
  // catch (Exception e)
  // {
  // throw new IOException(e);
  // }
  // }
  // //Write attachments
  // for (String name : event.getResult().getOutboundAttachmentNames())
  // {
  // DataHandler dh = event.getResult().getOutboundAttachment(name);
  // partStream.startPart(dh.getContentType());
  // partStream.getOut().write(IOUtils.toByteArray(dh.getInputStream()));
  // }
  // }
  // finally
  // {
  // partStream.close();
  // }
  // }
  // };
  //
  // }

}
