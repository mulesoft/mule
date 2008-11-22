/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.esb;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.model.Model;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.component.PooledJavaComponent;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.example.loanbroker.AsynchronousLoanBroker;
import org.mule.example.loanbroker.bank.Bank;
import org.mule.example.loanbroker.credit.CreditAgencyService;
import org.mule.example.loanbroker.lender.DefaultLender;
import org.mule.example.loanbroker.routers.BankQuotesResponseAggregator;
import org.mule.example.loanbroker.transformers.CreditProfileXmlToCreditProfile;
import org.mule.example.loanbroker.transformers.LoanQuoteRequestToCreditProfileArgs;
import org.mule.example.loanbroker.transformers.RestRequestToCustomerRequest;
import org.mule.example.loanbroker.transformers.SetLendersAsRecipients;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.object.PrototypeObjectFactory;
import org.mule.routing.binding.DefaultBindingCollection;
import org.mule.routing.binding.DefaultInterfaceBinding;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.routing.outbound.StaticRecipientList;
import org.mule.routing.response.DefaultResponseRouterCollection;
import org.mule.transport.ejb.EjbConnector;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.transport.jms.transformers.ObjectToJMSMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoanBrokerEsbConfigurationBuilder extends AbstractConfigurationBuilder implements ConfigurationBuilder
{

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        // Set defaults
        new DefaultsConfigurationBuilder().configure(muleContext);

        MuleRegistry registry = muleContext.getRegistry();

        // Connectors
        EjbConnector ejbConnector = new EjbConnector();
        ejbConnector.setName("ejbConnector");
        ejbConnector.setSecurityPolicy("security.policy");
        ejbConnector.setJndiInitialFactory("org.openejb.client.LocalInitialContextFactory");
        Map<String, String> jndiProviderProperties = new HashMap<String, String>();
        jndiProviderProperties.put("openejb.base", "${openejb.base}");
        jndiProviderProperties.put("openejb.configuration", "${openejb.configuration}");
        jndiProviderProperties.put("logging.conf", "${logging.conf}");
        jndiProviderProperties.put("openejb.nobanner", "${openejb.nobanner}");
        ejbConnector.setJndiProviderProperties(jndiProviderProperties);

        ActiveMQJmsConnector activeMQJmsConnector = new ActiveMQJmsConnector();
        activeMQJmsConnector.setName("activeMQJmsConnector");
        registry.registerConnector(activeMQJmsConnector);

        // Global Transformers
        Transformer RestRequestToCustomerRequest = new RestRequestToCustomerRequest();
        RestRequestToCustomerRequest.setName("RestRequestToCustomerRequest");
        registry.registerTransformer(RestRequestToCustomerRequest);
        Transformer LoanQuoteRequestToCreditProfileArgs = new LoanQuoteRequestToCreditProfileArgs();
        registry.registerTransformer(LoanQuoteRequestToCreditProfileArgs);
        Transformer CreditProfileXmlToCreditProfile = new CreditProfileXmlToCreditProfile();
        registry.registerTransformer(CreditProfileXmlToCreditProfile);
        Transformer SetLendersAsRecipients = new SetLendersAsRecipients();
        registry.registerTransformer(SetLendersAsRecipients);
        Transformer ObjectToJMSMessage = new ObjectToJMSMessage();
        registry.registerTransformer(ObjectToJMSMessage);

        // Global Endpoints
        EndpointBuilder CustomerRequestsREST = new EndpointURIEndpointBuilder(new URIBuilder(
            "jetty:rest://localhost:8888/loanbroker"), muleContext);
        registry.registerEndpointBuilder("CustomerRequestsREST", CustomerRequestsREST);
        EndpointBuilder CustomerRequests = new EndpointURIEndpointBuilder(new URIBuilder("vm://customer.requests"),
            muleContext);
        registry.registerEndpointBuilder("CustomerRequests", CustomerRequests);
        EndpointBuilder LoanQuotes = new EndpointURIEndpointBuilder(new URIBuilder("axis:http://localhost:10080/mule"),
            muleContext);
        registry.registerEndpointBuilder("LoanQuotes", LoanQuotes);
        EndpointBuilder CustomerResponses = LoanQuotes;
        registry.registerEndpointBuilder("CustomerResponses", CustomerResponses);
        EndpointBuilder CreditAgencyGateway = new EndpointURIEndpointBuilder(new URIBuilder("jms://esb.credit.agency"),
            muleContext);
        registry.registerEndpointBuilder("CreditAgencyGateway", CreditAgencyGateway);
        EndpointBuilder CreditAgency = new EndpointURIEndpointBuilder(new URIBuilder(
            "ejb://localhost:1099/local/CreditAgency?method=getCreditProfile"), muleContext);
        registry.registerEndpointBuilder("CreditAgency", CreditAgency);
        EndpointBuilder LenderGateway = new EndpointURIEndpointBuilder(new URIBuilder("jms://esb.lender.service"),
            muleContext);
        registry.registerEndpointBuilder("LenderGateway", LenderGateway);
        EndpointBuilder LenderService = new EndpointURIEndpointBuilder(new URIBuilder("vm://lender.service"),
            muleContext);
        registry.registerEndpointBuilder("LenderService", LenderService);
        EndpointBuilder BankingGateway = new EndpointURIEndpointBuilder(new URIBuilder("jms://esb.banks"), muleContext);

        EndpointBuilder bank1 = new EndpointURIEndpointBuilder(new URIBuilder(
            "axis:http://localhost:10080/mule/TheBank1?method=getLoanQuote"), muleContext);
        registry.registerEndpointBuilder("bank1", bank1);
        EndpointBuilder bank2 = new EndpointURIEndpointBuilder(new URIBuilder(
            "axis:http://localhost:20080/mule/TheBank2?method=getLoanQuote"), muleContext);
        registry.registerEndpointBuilder("bank2", bank2);
        EndpointBuilder bank3 = new EndpointURIEndpointBuilder(new URIBuilder(
            "axis:http://localhost:30080/mule/TheBank3?method=getLoanQuote"), muleContext);
        registry.registerEndpointBuilder("bank3", bank3);
        EndpointBuilder bank4 = new EndpointURIEndpointBuilder(new URIBuilder(
            "axis:http://localhost:40080/mule/TheBank4?method=getLoanQuote"), muleContext);
        registry.registerEndpointBuilder("bank4", bank4);

        EndpointBuilder bank1In = new EndpointURIEndpointBuilder(new URIBuilder("axis:http://localhost:10080/mule"),
            muleContext);
        registry.registerEndpointBuilder("bank1In", bank1In);
        EndpointBuilder bank2In = new EndpointURIEndpointBuilder(new URIBuilder("axis:http://localhost:20080/mule"),
            muleContext);
        registry.registerEndpointBuilder("bank2In", bank2In);
        EndpointBuilder bank3In = new EndpointURIEndpointBuilder(new URIBuilder("axis:http://localhost:30080/mule"),
            muleContext);
        registry.registerEndpointBuilder("bank3In", bank3In);
        EndpointBuilder bank4In = new EndpointURIEndpointBuilder(new URIBuilder("axis:http://localhost:40080/mule"),
            muleContext);
        registry.registerEndpointBuilder("bank4In", bank4In);

        // Model
        Model model = new SedaModel();
        model.setName("model");
        registry.registerModel(model);

        // LoanBroker Service
        Service loanBrokerService = new SedaService();
        loanBrokerService.setName("LoanBroker");
        loanBrokerService.setModel(model);
        loanBrokerService.setComponent(new PooledJavaComponent(new PrototypeObjectFactory(AsynchronousLoanBroker.class)));
        // in
        InboundRouterCollection loanBrokerServiceInbound = new DefaultInboundRouterCollection();
        loanBrokerServiceInbound.addEndpoint(CustomerRequestsREST.buildInboundEndpoint());
        EndpointBuilder eb = (EndpointBuilder) CustomerRequests.clone();
        eb.addTransformer(RestRequestToCustomerRequest);
        loanBrokerServiceInbound.addEndpoint(eb.buildInboundEndpoint());
        loanBrokerService.setInboundRouter(loanBrokerServiceInbound);
        // out
        OutboundRouterCollection loanBrokerServiceOutbound = new DefaultOutboundRouterCollection();
        OutboundPassThroughRouter outboundPassThroughRouter = new OutboundPassThroughRouter();
        outboundPassThroughRouter.addEndpoint(CreditAgency.buildOutboundEndpoint());
        loanBrokerServiceOutbound.addRouter(outboundPassThroughRouter);
        loanBrokerService.setOutboundRouter(loanBrokerServiceOutbound);
        // reply
        ResponseRouterCollection responseRouterCollection = new DefaultResponseRouterCollection();
        responseRouterCollection.addEndpoint(LoanQuotes.buildInboundEndpoint());
        responseRouterCollection.addRouter(new BankQuotesResponseAggregator());
        loanBrokerService.setResponseRouter(responseRouterCollection);

        registry.registerService(loanBrokerService);

        // CreditAgencyGatewayService Service
        Service creditAgencyGatewayService = new SedaService();
        creditAgencyGatewayService.setName("CreditAgencyGatewayService");
        creditAgencyGatewayService.setModel(model);
        PooledJavaComponent component = new PooledJavaComponent(new PrototypeObjectFactory(CreditAgencyGateway.class));
        creditAgencyGatewayService.setComponent(component);
        // in
        InboundRouterCollection creditAgencyGatewayServiceInbound = new DefaultInboundRouterCollection();
        creditAgencyGatewayServiceInbound.addEndpoint(CreditAgencyGateway.buildInboundEndpoint());
        creditAgencyGatewayService.setInboundRouter(creditAgencyGatewayServiceInbound);
        //binding
        EndpointBuilder eb2 = (EndpointBuilder) CreditAgency.clone();
        eb2.addTransformer(LoanQuoteRequestToCreditProfileArgs);
        eb2.setSynchronous(true);
        List<Transformer> responseTransformers = new ArrayList<Transformer>();
        responseTransformers.add(CreditProfileXmlToCreditProfile);
        eb2.setResponseTransformers(responseTransformers);
        //Create Binding
        component.setBindingCollection(new DefaultBindingCollection());
        DefaultInterfaceBinding binding = new DefaultInterfaceBinding();
        binding.setInterface(CreditAgencyService.class);
        binding.setMethod("getCreditProfile");
        binding.setEndpoint(eb2.buildOutboundEndpoint());
        component.getBindingCollection().addRouter(binding);

        // out
        OutboundRouterCollection creditAgencyGatewayServiceOutbound = new DefaultOutboundRouterCollection();
        FilteringOutboundRouter creditAgencyGatewayServiceOutboundRouter = new FilteringOutboundRouter();

        creditAgencyGatewayServiceOutboundRouter.addEndpoint(LenderGateway.buildOutboundEndpoint());
        creditAgencyGatewayServiceOutbound.addRouter(creditAgencyGatewayServiceOutboundRouter);
        creditAgencyGatewayService.setOutboundRouter(creditAgencyGatewayServiceOutbound);

        registry.registerService(creditAgencyGatewayService);

        // LenderGatewayService Service
        Service lenderGatewayService = new SedaService();
        lenderGatewayService.setName("CreditAgencyGatewayService");
        lenderGatewayService.setModel(model);
        // in
        InboundRouterCollection lenderGatewayServiceInbound = new DefaultInboundRouterCollection();
        lenderGatewayServiceInbound.addEndpoint(LenderGateway.buildInboundEndpoint());
        lenderGatewayService.setInboundRouter(lenderGatewayServiceInbound);
        // out
        OutboundRouterCollection lenderGatewayServiceInboundOutbound = new DefaultOutboundRouterCollection();
        FilteringOutboundRouter lenderGatewayServiceInboundOutboundRouter = new FilteringOutboundRouter();
        EndpointBuilder eb3 = (EndpointBuilder) LenderService.clone();
        eb3.setSynchronous(true);
        lenderGatewayServiceInboundOutboundRouter.addEndpoint(eb3.buildOutboundEndpoint());
        EndpointBuilder eb4 = (EndpointBuilder) BankingGateway.clone();
        eb4.addTransformer(SetLendersAsRecipients);
        eb4.addTransformer(ObjectToJMSMessage);
        lenderGatewayServiceInboundOutboundRouter.addEndpoint(eb4.buildOutboundEndpoint());
        lenderGatewayServiceInboundOutbound.addRouter(lenderGatewayServiceInboundOutboundRouter);
        lenderGatewayService.setOutboundRouter(lenderGatewayServiceInboundOutbound);

        registry.registerService(lenderGatewayService);

        // LenderServiceService Service
        Service lenderServiceService = new SedaService();
        lenderServiceService.setName("LenderServiceService");
        lenderServiceService.setModel(model);
        lenderGatewayService.setComponent(new PooledJavaComponent(new PrototypeObjectFactory(DefaultLender.class)));
        // in
        InboundRouterCollection lenderServiceServiceInbound = new DefaultInboundRouterCollection();
        lenderServiceServiceInbound.addEndpoint(LenderService.buildInboundEndpoint());
        lenderServiceService.setInboundRouter(lenderServiceServiceInbound);

        registry.registerService(lenderGatewayService);

        // BankingGatewayService Service
        Service bankingGatewayService = new SedaService();
        bankingGatewayService.setName("BankingGatewayService");
        bankingGatewayService.setModel(model);
        // in
        InboundRouterCollection bankingGatewayServiceInbound = new DefaultInboundRouterCollection();
        bankingGatewayServiceInbound.addEndpoint(BankingGateway.buildInboundEndpoint());
        bankingGatewayService.setInboundRouter(bankingGatewayServiceInbound);
        // out
        OutboundRouterCollection bankingGatewayServiceOutbound = new DefaultOutboundRouterCollection();
        StaticRecipientList staticRecipientList = new StaticRecipientList();
        staticRecipientList.setReplyTo("LoanQuotes");
        staticRecipientList.setFilter(new MessagePropertyFilter("recipients!=null"));

        registry.registerService(lenderGatewayService);

        // Bank 1 Service
        Service bank1Service = new SedaService();
        bank1Service.setName("TheBank1");
        bank1Service.setModel(model);
        bank1Service.setComponent(new PooledJavaComponent(new PrototypeObjectFactory(Bank.class)));
        InboundRouterCollection bank1ServiceInbound = new DefaultInboundRouterCollection();
        bank1ServiceInbound.addEndpoint(bank1In.buildInboundEndpoint());
        bank1Service.setInboundRouter(bank1ServiceInbound);
        registry.registerService(bank1Service);

        // Bank 2 Service
        Service bank2Service = new SedaService();
        bank2Service.setName("TheBank2");
        bank2Service.setModel(model);
        bank2Service.setComponent(new PooledJavaComponent(new PrototypeObjectFactory(Bank.class)));
        InboundRouterCollection bank2ServiceInbound = new DefaultInboundRouterCollection();
        bank2ServiceInbound.addEndpoint(bank2In.buildInboundEndpoint());
        bank2Service.setInboundRouter(bank2ServiceInbound);
        registry.registerService(bank2Service);

        // Bank 3 Service
        Service bank3Service = new SedaService();
        bank3Service.setName("TheBank3");
        bank3Service.setModel(model);
        bank3Service.setComponent(new PooledJavaComponent(new PrototypeObjectFactory(Bank.class)));
        InboundRouterCollection bank3ServiceInbound = new DefaultInboundRouterCollection();
        bank3ServiceInbound.addEndpoint(bank3In.buildInboundEndpoint());
        bank3Service.setInboundRouter(bank3ServiceInbound);
        registry.registerService(bank3Service);

        // Bank 4 Service
        Service bank4Service = new SedaService();
        bank4Service.setName("TheBank4");
        bank4Service.setModel(model);
        bank4Service.setComponent(new PooledJavaComponent(new PrototypeObjectFactory(Bank.class)));
        InboundRouterCollection bank4ServiceInbound = new DefaultInboundRouterCollection();
        bank4ServiceInbound.addEndpoint(bank4In.buildInboundEndpoint());
        bank4Service.setInboundRouter(bank4ServiceInbound);
        registry.registerService(bank4Service);
    }

    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception 
    {
        // nothing to do
    }
}
