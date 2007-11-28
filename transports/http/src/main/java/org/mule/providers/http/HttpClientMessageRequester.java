/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.providers.AbstractMessageRequester;
import org.mule.providers.http.i18n.HttpMessages;
import org.mule.providers.http.transformers.HttpClientMethodResponseToObject;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.StringUtils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Rquests Mule events over HTTP.
 */
public class HttpClientMessageRequester extends AbstractMessageRequester
{

    private final HttpConnector connector;
    private volatile HttpClient client = null;
    private final UMOTransformer receiveTransformer;

    public HttpClientMessageRequester(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (HttpConnector) endpoint.getConnector();
        this.receiveTransformer = new HttpClientMethodResponseToObject();
    }

    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            client = connector.doClientConnect();
        }
    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doRequest(long timeout) throws Exception
    {
        HttpMethod httpMethod = new GetMethod(endpoint.getEndpointURI().getAddress());
        connector.setupClientAuthorization(null, httpMethod, client, endpoint);

        boolean releaseConn = false;
        try
        {
            HttpClient client = new HttpClient();
            client.executeMethod(httpMethod);

            if (httpMethod.getStatusCode() == HttpStatus.SC_OK)
            {
                UMOMessage res = (UMOMessage) receiveTransformer.transform(httpMethod);
                if (StringUtils.EMPTY.equals(res.getPayload()))
                {
                    releaseConn = true;
                }
                return res;
            }
            else
            {
                releaseConn = true;
                throw new ReceiveException(
                    HttpMessages.requestFailedWithStatus(httpMethod.getStatusLine().toString()),
                    endpoint, timeout);
            }
        }
        catch (ReceiveException e)
        {
            releaseConn = true;
            throw e;
        }
        catch (Exception e)
        {
            releaseConn = true;
            throw new ReceiveException(endpoint, timeout, e);
        }
        finally
        {
            if (releaseConn)
            {
                httpMethod.releaseConnection();
            }
        }
    }

    protected void doDispose()
    {
        // template method
    }
}