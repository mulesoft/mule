/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.ReceiveException;
import org.mule.compatibility.core.transport.AbstractMessageRequester;
import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.compatibility.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.util.StringUtils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Rquests Mule events over HTTP.
 */
public class HttpClientMessageRequester extends AbstractMessageRequester {

  protected final HttpConnector connector;
  protected volatile HttpClient client = null;
  protected final Transformer receiveTransformer;

  public HttpClientMessageRequester(InboundEndpoint endpoint) {
    super(endpoint);
    this.connector = (HttpConnector) endpoint.getConnector();
    this.receiveTransformer = new HttpClientMethodResponseToObject();
    this.receiveTransformer.setMuleContext(getEndpoint().getMuleContext());
  }

  @Override
  protected void doConnect() throws Exception {
    if (client == null) {
      client = connector.doClientConnect();
    }
  }

  @Override
  protected void doDisconnect() throws Exception {
    client = null;
  }

  /**
   * Make a specific request to the underlying transport
   *
   * @param timeout the maximum time the operation should block before returning. The call should return immediately if there is
   *        data available. If no data becomes available before the timeout elapses, null will be returned
   * @return the result of the request wrapped in a MuleMessage object. Null will be returned if no data was avaialable
   * @throws Exception if the call to the underlying protocal cuases an exception
   */
  @Override
  protected MuleMessage doRequest(long timeout) throws Exception {
    HttpMethod httpMethod = new GetMethod(endpoint.getEndpointURI().getAddress());
    connector.setupClientAuthorization(null, httpMethod, client, endpoint);

    boolean releaseConn = false;
    try {
      HttpClient client = new HttpClient();
      client.executeMethod(httpMethod);

      if (httpMethod.getStatusCode() == HttpStatus.SC_OK) {
        MuleMessage res = (MuleMessage) receiveTransformer.transform(httpMethod);
        if (StringUtils.EMPTY.equals(res.getPayload())) {
          releaseConn = true;
        }
        return res;
      } else {
        releaseConn = true;
        throw new ReceiveException(HttpMessages.requestFailedWithStatus(httpMethod.getStatusLine().toString()), endpoint,
                                   timeout);
      }
    } catch (ReceiveException e) {
      releaseConn = true;
      throw e;
    } catch (Exception e) {
      releaseConn = true;
      throw new ReceiveException(endpoint, timeout, e);
    } finally {
      if (releaseConn) {
        httpMethod.releaseConnection();
      }
    }
  }

  @Override
  protected void doDispose() {
    // template method
  }
}
