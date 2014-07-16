/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.feature;

import org.mule.module.cxf.support.ProxyGZIPInInterceptor;
import org.mule.module.cxf.support.ProxyGZIPOutInterceptor;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;


/**
 * This class is used to control GZIP compression of messages when proxying
 * a web service call.
 *
 * If the invoking web service returns a compressed response
 * the proxy client will decompress it to work with the raw envelope or body in the flow
 * and the proxy service will compress it again before sending it back to the client.
 *
 * If the client invokes the service with a compressed request, the proxy service is
 * responsible for decompressing it to work with the raw request and the client
 * will compress it back before invoking the end service.
 *
 * In each case the property being verified is the Content-Encoding of the request
 * and the response.
 *
 */
@NoJSR250Annotations
public class ProxyGZIPFeature extends AbstractFeature
{
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus)
    {
        provider.getInInterceptors().add(new ProxyGZIPInInterceptor());
        ProxyGZIPOutInterceptor out = new ProxyGZIPOutInterceptor();
        provider.getOutInterceptors().add(out);
        provider.getOutFaultInterceptors().add(out);
    }

}
