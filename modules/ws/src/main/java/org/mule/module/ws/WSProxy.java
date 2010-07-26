/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws;

import java.net.InetAddress;
import java.net.URL;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.processor.FlowConstructStatisticsMessageObserver;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;

/**
 * This class is implemented to act as a Proxy for a Web Service. It listens for
 * requests on the inbound endpoint and if it encounters the "WSDL" property in the
 * address, it will fetch the WSDL from the original web service and return it back.
 * In case the wsdlFile property is set, when the WSProxyService encounters a request
 * for the wsdl, instead of fetching the WSDL from the original web service, it will
 * return back the file expressed in the property. When a normal SOAP request is
 * encountered, it will forward the call to the web service with no modifications to
 * the SOAP message. The outbound router of this class must include the address of
 * the webservice to be proxied. No need to include the method name as a parameter in
 * the address, since it will be in the SOAP message as well. Furthermore a property
 * named uriWsdl can optionally be set which as the name suggests, indicate the URL
 * of the WSDL for the service. If this property is not set, the address of the WSDL
 * will be assumed to be the value of uriWebservice followed by "?WSDL".
 */
public class WSProxy extends AbstractFlowConstruct
{
    private final MessageProcessor proxyMessageProcessor;

    // TODO (DDO) add support for single outbound endpoint

    public WSProxy(MuleContext muleContext, String name, MessageSource messageSource, String wsdlContents)
        throws MuleException
    {
        this(muleContext, name, messageSource, new StaticWsdlRequestProcessor(wsdlContents));
    }

    public WSProxy(MuleContext muleContext, String name, MessageSource messageSource, URL wsdlUrl)
        throws MuleException
    {
        this(muleContext, name, messageSource, new DynamicWsdlProcessor(wsdlUrl));
    }

    private WSProxy(MuleContext muleContext,
                    String name,
                    MessageSource messageSource,
                    MessageProcessor proxyMessageProcessor) throws MuleException
    {
        super(name, muleContext);

        if (messageSource == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("messageSource can't be null on: " + this.toString()));
        }

        super.setMessageSource(messageSource);
        this.proxyMessageProcessor = proxyMessageProcessor;
    }

    @Override
    protected void configureMessageProcessors(InterceptingChainMessageProcessorBuilder builder)
    {
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageObserver());
        builder.chain(proxyMessageProcessor);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        if ((messageSource instanceof InboundEndpoint)
            && (!((InboundEndpoint) messageSource).getExchangePattern().equals(
                MessageExchangePattern.REQUEST_RESPONSE)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("SimpleService only works with a request-response inbound endpoint."));
        }

        // TODO (DDO) ensure one single synchronous outbound
    }

    private static abstract class AbstractRequestProcessor implements MessageProcessor
    {
        private static final String HTTP_REQUEST = "http.request";
        private static final String WSDL_PARAM_1 = "?wsdl";
        private static final String WSDL_PARAM_2 = "&wsdl";

        protected final Log logger = LogFactory.getLog(getClass());

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MuleMessage message = event.getMessage();

            // retrieve the original HTTP request. This will be used to check if the
            // user asked for the WSDL or a service method.
            String httpRequest = message.<String> getInboundProperty(HTTP_REQUEST).toLowerCase();

            // check if the inbound endpoint contains the WSDL parameter
            if (isWsdlRequest(httpRequest))
            {
                return buildWsdlResult(event);
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Forwarding SOAP message");
            }

            return event;
        }

        private MuleEvent buildWsdlResult(MuleEvent event) throws MuleException
        {
            // the processing is stopped so that the result is not passed through
            // the outbound router but will be passed back as a result
            event.setStopFurtherProcessing(true);

            try
            {
                String wsdlContents = getWsdlContents(event);
                event.getMessage().setPayload(wsdlContents);
                return event;
            }
            catch (Exception e)
            {
                throw new MessagingException(
                    MessageFactory.createStaticMessage("Impossible to retrieve WSDL for proxied service"),
                    event.getMessage(), e);
            }
        }

        private boolean isWsdlRequest(String httpRequest)
        {
            return (httpRequest.indexOf(WSDL_PARAM_1) != -1) || (httpRequest.indexOf(WSDL_PARAM_2) != -1);
        }

        protected abstract String getWsdlContents(MuleEvent event) throws Exception;
    }

    private static class StaticWsdlRequestProcessor extends AbstractRequestProcessor
    {
        private final String wsdlContents;

        StaticWsdlRequestProcessor(String wsdlContents)
        {
            Validate.notEmpty(wsdlContents, "wsdlContents can't be empty");
            this.wsdlContents = wsdlContents;
        }

        @Override
        protected String getWsdlContents(MuleEvent event) throws Exception
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Serving static WSDL");
            }

            return wsdlContents;
        }
    }

    private static class DynamicWsdlProcessor extends AbstractRequestProcessor
    {
        private static final String LOCALHOST = "localhost";
        private final String wsdlAddress;

        DynamicWsdlProcessor(URL wsdlUrl)
        {
            Validate.notNull(wsdlUrl, "wsdlUrl can't be null");
            wsdlAddress = wsdlUrl.toExternalForm();
            logger.info("Using url " + wsdlAddress + " as WSDL");
        }

        // will be used when support for outbound endpoint will be added
        @SuppressWarnings("unused")
        public DynamicWsdlProcessor(OutboundEndpoint outboundEndpoint)
        {
            Validate.notNull(outboundEndpoint, "outboundEndpoint can't be null");

            String urlWebservice = outboundEndpoint.getEndpointURI().getAddress();

            // remove any params from the url
            int paramIndex = urlWebservice.indexOf("?");
            if (paramIndex != -1)
            {
                urlWebservice = urlWebservice.substring(0, paramIndex);
            }

            wsdlAddress = urlWebservice.concat("?wsdl");
            logger.info("Defaulting to: " + wsdlAddress);
        }

        @Override
        protected String getWsdlContents(MuleEvent event) throws Exception
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Retrieving WSDL from web service");
            }

            String wsdlString;

            MuleContext muleContext = event.getMuleContext();
            InboundEndpoint webServiceEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint(wsdlAddress);

            MuleMessage replyWSDL = webServiceEndpoint.request(event.getTimeout());
            wsdlString = replyWSDL.getPayloadAsString();

            // create a new mule message with the new WSDL
            String realWsdlAddress = wsdlAddress.split("\\?")[0];
            String proxyWsdlAddress = event.getEndpoint().getEndpointURI().getAddress();
            wsdlString = wsdlString.replaceAll(realWsdlAddress, proxyWsdlAddress);

            if (wsdlString.indexOf(LOCALHOST) > -1)
            {
                wsdlString = wsdlString.replaceAll(LOCALHOST, InetAddress.getLocalHost().getHostName());
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("WSDL retrieved successfully");
            }

            return wsdlString;
        }
    }
}
