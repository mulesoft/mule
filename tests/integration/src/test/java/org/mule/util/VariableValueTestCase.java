/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;


import org.junit.Rule;
import org.junit.Test;


public class VariableValueTestCase extends FunctionalTestCase
{

    private static final String URL = "vm://%s";
    private static final String PATH1 = "path1";
    private static final String PATH2 = "path2";
    private static final String PATH3 = "path3";
    private static final String PATH4 = "path4";

    @Rule
    public SystemProperty path1SystemProperty = new SystemProperty("path1", PATH1);

    @Rule
    public SystemProperty path2SystemProperty = new SystemProperty("path2", PATH2);

    @Rule
    public SystemProperty path3SystemProperty = new SystemProperty("path3", PATH3);

    @Rule
    public SystemProperty path4SystemProperty = new SystemProperty("path4", PATH4);

    @Rule
    public SystemProperty variableValueSystemProperty = new SystemProperty("variableValueWithADollarSign", "12345@$6789");

    @Rule
    public SystemProperty variableValue2SystemProperty = new SystemProperty("variableValueWithManyDollarSign", "12345@$67$89$10$");

    @Rule
    public SystemProperty variableValue3SystemProperty = new SystemProperty("variableValueWithManyDollarSignAndBlackSlashes", "\\12345@\\$67$89$10$12\\");


    @Override
    protected String getConfigFile()
    {
        return "variable-value.xml";
    }


    @Test
    public void testValueOfVariableAlone() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        final String url = String.format(URL, PATH1);

        MuleMessage response = client.send(url, TEST_MESSAGE, null);

        assertThat(response.getPayloadAsString(), is(variableValueSystemProperty.getValue()));
    }

    @Test
    public void testValueOfVariableEmbeddedInPayloadWithADollarSign() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        final String url = String.format(URL, PATH2);
        MuleMessage response = client.send(url, TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), containsString(variableValueSystemProperty.getValue()));
    }

    @Test
    public void testValueOfVariableEmbeddedInPayloadWithManyDollarSign() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        final String url = String.format(URL, PATH3);
        MuleMessage response = client.send(url, TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), containsString(variableValue2SystemProperty.getValue()));
    }

    @Test
    public void testValueOfVariableEmbeddedInPayloadWithManyDollarSignAndBlackSlashes() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        final String url = String.format(URL, PATH4);
        MuleMessage response = client.send(url, TEST_MESSAGE, null);
        assertThat(response.getPayloadAsString(), containsString(variableValue3SystemProperty.getValue()));
    }
}
