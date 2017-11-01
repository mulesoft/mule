/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_USE_CONNECTOR_TO_RETRIEVE_WSDL;

import org.mule.tck.junit4.rule.SystemProperty;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.requester.HttpRequesterConfig;

import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.junit.Rule;
import org.junit.Test;

/**
 * This tests "mocks" an HTTPS server through which a wsdl with imported namespaces file is served. The initialization
 * of the ws consumer involved in the test app should be routed through the http requester configured with an insecure
 * TLS context. If an URLConnection is used instead of the http requester, the TLS context will not be used.
 */
public abstract class AbstractWSConsumerImportedNamespaceTlsTestCase extends AbstractWSDLHttpImportedSchemaServerTlsTestCase
{

    @Rule
    public SystemProperty useConnectorToRetrieveWsdl = new SystemProperty(MULE_USE_CONNECTOR_TO_RETRIEVE_WSDL, "true");

    private static final String NO_PARAMS_WITH_HEADER = "noParamsWithHeader";
    private static final String TEST_PORT = "TestPort";
    private static final String TEST_SERVICE = "TestService";
    private static final String EXPECTED_BODY_PATTERN = "<ns:noParamsWithHeader xmlns:ns=\"http://consumer.ws.module.mule.org/\" />";

    @Test
    public void consumerPresentInRegistry() throws Exception
    {
        Map<String, WSConsumer> consumers = muleContext.getRegistry().lookupByType(WSConsumer.class);
        // if one consumer is present in the registry, the config was correctly initialized
        assertThat(consumers.values().size(), equalTo(1));
        // verify that the request body is correct
        WSConsumer wsConsumer = consumers.values().iterator().next();
        WSConsumerConfig wsConsumerConfig = wsConsumer.getConfig();
        MuleWSDLLocator locator = getLocator(wsConsumerConfig);
        Definition wsdlDefinition = getDefinition(locator);
        RequestBodyGenerator requestBodyGenerator = new RequestBodyGenerator(wsdlDefinition, locator);
        Service service = wsdlDefinition.getService(new QName(wsdlDefinition.getTargetNamespace(), TEST_SERVICE));
        Port port = service.getPort(TEST_PORT);
        Binding binding = port.getBinding();
        BindingOperation bindingOperation = binding.getBindingOperation(NO_PARAMS_WITH_HEADER, null, null);
        String requestBody = requestBodyGenerator.generateRequestBody(bindingOperation);

        assertThat(requestBody, equalTo(EXPECTED_BODY_PATTERN));
    }

    private MuleWSDLLocator getLocator(WSConsumerConfig config) throws Exception
    {
        HttpRequesterConfig httpRequesterConfig = config.getConnectorConfig();
        String url = config.getWsdlLocation();

        MuleWSDLLocatorConfig locatorConfig = createWSDLLocator(httpRequesterConfig, url);
        return new MuleWSDLLocator(locatorConfig);
    }

    private MuleWSDLLocatorConfig createWSDLLocator(HttpRequesterConfig httpRequesterConfig, String url) throws InitialisationException
    {
        MuleWSDLLocatorConfig locatorConfig = null;
        locatorConfig = new MuleWSDLLocatorConfig.Builder()
                                                           .setBaseURI(url)
                                                           .setTlsContextFactory(httpRequesterConfig.getTlsContext())
                                                           .setContext(muleContext)
                                                           .setUseConnectorToRetrieveWsdl(true)
                                                           .setProxyConfig(httpRequesterConfig.getProxyConfig())
                                                           .build();
        return locatorConfig;
    }

    private Definition getDefinition(WSDLLocator wsdlLocator) throws Exception
    {
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();

        Definition wsdlDefinition = wsdlReader.readWSDL(wsdlLocator);
        return wsdlDefinition;
    }
}
