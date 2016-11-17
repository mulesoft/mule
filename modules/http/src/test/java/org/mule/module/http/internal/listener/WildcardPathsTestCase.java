/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.listener;


import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class WildcardPathsTestCase extends FunctionalTestCase
{
    private MuleContext muleContext;

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    private static final String HOST ="http://localhost:%s/%s";
	@Test
    public void testFirstLevelWildCardWithPathEmpty() throws Exception
    {
        Assert.assertThat("V1 Flow invoked", Is.is(doGet(createURL(""))));
    }
	
	@Test
    public void testFirstLevelWildcardWithOneLevelPath() throws Exception
    {
       Assert.assertThat("V1 Flow invoked",Is.is(doGet(createURL("taxes"))));
    }

    @Test
    public void testFirstLevelWildcardWithTwoLevelPath() throws Exception
    {
        Assert.assertThat("V1 Flow invoked",Is.is(doGet(createURL("taxes/healtcheck"))));
    }

	@Test
    public void testFirstLevelWildcardWithTwoLevelPath2() throws Exception
    {
        Assert.assertThat("V1 Flow invoked",Is.is(doGet(createURL("taxes/1"))));
    }


    @Test
    public void testTwoLevelWildcardWithOneLevelPath() throws Exception
    {
        Assert.assertThat("v2 flow invoked",Is.is(doGet(createURL("v2"))));
    }

    @Test
    public void testTwoLevelWildcardWithTwoLevelPath() throws Exception
    {
        Assert.assertThat("v2 flow invoked",Is.is(doGet(createURL("v2/taxes"))));
    }

    @Test
    public void testWildcardWithTwoLevelPath2() throws Exception
    {
        Assert.assertThat("v2 flow invoked",Is.is(doGet(createURL("v2/console"))));
    }

    @Test
    public void testTwoLevelWildcardWithThreeLevelPath() throws Exception
    {
        Assert.assertThat("v2 flow invoked",Is.is(doGet(createURL("v2/taxes/1"))));
    }

    @Test
    public void testListenerWithThreeLevelPath() throws Exception
    {
        Assert.assertThat("V2 - Healthcheck",Is.is(doGet(createURL("v2/taxes/healthcheck"))));
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder("HttpTestPathsWildcard.xml");
    }


    private String createURL(String path){
        return String.format(HOST,listenPort.getNumber(),path);
    }


    private String doGet(String urlString){
		 URL url;
		 String inputLine=null;
		try {
			url = new URL(urlString);
		    URLConnection yc = url.openConnection();
	        BufferedReader in = new BufferedReader(
	                                new InputStreamReader(
	                                yc.getInputStream()));
	        inputLine = in.readLine();

	        in.close();
		} catch (IOException e) {
		
		}
		return inputLine;
	}
}
