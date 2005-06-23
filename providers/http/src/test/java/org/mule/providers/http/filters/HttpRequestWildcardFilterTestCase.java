/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.http.filters;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.mule.MuleManager;
import org.mule.components.simple.EchoComponent;
import org.mule.config.PoolingProfile;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleModel;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.service.ConnectorFactory;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOConnector;

/**
 * @author Jack Hung
 * @version $Revision$
 */
public class HttpRequestWildcardFilterTestCase extends NamedTestCase {

	private static final String HTTP_LOCALHOST_60198 = "http://localhost:60198";
	private static final String TEST_MESSAGE = "Hello=World";
	
	protected void setUp() throws Exception {
		super.setUp();
		if (MuleManager.isInstanciated()) {
			MuleManager.getInstance().dispose();
		}
		UMOManager manager = MuleManager.getInstance();
		MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().getPoolingProfile()
                   .setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);
        manager.setModel(new MuleModel());
		
        MuleDescriptor descriptor = createInDescriptor(
				"httpIn", EchoComponent.class.getName());
		manager.getModel().registerComponent(descriptor);
		manager.start();
	}

	private MuleDescriptor createInDescriptor(String name, String implementation) throws UMOException {
		MuleDescriptor descriptor = new MuleDescriptor();
		descriptor.setExceptionListener(new DefaultExceptionStrategy());
		descriptor.setName(name);
		descriptor.setImplementation(implementation);
		descriptor.setInboundEndpoint(buildEndpoint(
				HTTP_LOCALHOST_60198));
		return descriptor;
	}

	private MuleEndpoint buildEndpoint(String urlStr) throws EndpointException, MalformedEndpointException, UMOException {
		MuleEndpoint endpoint = new MuleEndpoint();
		endpoint.setEndpointURI(new MuleEndpointURI(urlStr));
		endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
		endpoint.setFilter(new NotFilter(new WildcardFilter("*xyz*")));
		//endpoint.setTransformer(new HttpRequestToString());
		endpoint.setConnector(buildConnector(urlStr));
		return endpoint;
	}

	private UMOConnector buildConnector(String urlStr) throws UMOException {
		HttpConnector connector = (HttpConnector) ConnectorFactory.createConnector(new MuleEndpointURI(urlStr));
		connector.getDispatcherThreadingProfile().setDoThreading(false);
		MuleManager.getInstance().registerConnector(connector);
		return connector;
	}

	
	public void testHttpPost() throws URISyntaxException, HttpRecoverableException, HttpException, IOException {
        URI uri = new URI(HTTP_LOCALHOST_60198);
        PostMethod postMethod = new PostMethod(uri.toString());
        postMethod.setRequestBody(TEST_MESSAGE);
        postMethod.setRequestContentLength(TEST_MESSAGE.length());
        HttpConnection cnn = new HttpConnection(uri.getHost(), uri.getPort());
        postMethod.execute(new HttpState(), cnn);
		System.out.println("PostResponse: " + 
        		postMethod.getResponseBodyAsString());
	}
	
	public void testHttpGetNotFilter() throws HttpRecoverableException, HttpException, URISyntaxException, IOException {
		String result = doHttpGet("hello"); 
		assertTrue(result.indexOf("hello") > -1);
	}

	public void testHttpGetFilter() throws HttpRecoverableException, HttpException, URISyntaxException, IOException {
		try {
			doHttpGet("xyz");
			fail("The response should be invalid as the filter has discarded the request");
		} catch (HttpRecoverableException e) {
			// This is ok
		}
	}

	protected String doHttpGet(String param) throws URISyntaxException, HttpRecoverableException, HttpException, IOException {
        URI uri = new URI(HTTP_LOCALHOST_60198);
        GetMethod getMethod = new GetMethod(uri.toString());
		getMethod.setQueryString("param=" + param);
        HttpConnection cnn = new HttpConnection(uri.getHost(), uri.getPort());
        getMethod.execute(new HttpState(), cnn);
        System.out.println("GetResponse: " + 
        		getMethod.getResponseBodyAsString());
		return getMethod.getResponseBodyAsString();
	}
}
