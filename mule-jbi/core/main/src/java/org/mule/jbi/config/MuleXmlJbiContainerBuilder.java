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
*
*/
package org.mule.jbi.config;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.ConfigurationException;
import org.mule.config.ReaderResource;
import org.mule.config.builders.AbstractDigesterConfiguration;
import org.mule.config.builders.ContainerReference;
import org.mule.config.builders.ObjectGetOrCreateRule;
import org.mule.config.converters.ConnectorConverter;
import org.mule.config.converters.EndpointConverter;
import org.mule.config.converters.EndpointURIConverter;
import org.mule.config.converters.TransactionFactoryConverter;
import org.mule.config.converters.TransformerConverter;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.components.AbstractComponent;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.response.ResponseMessageRouter;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.UMOInboundMessageRouter;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.transformer.UMOTransformer;
import org.xml.sax.Attributes;

import javax.jbi.JBIException;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleXmlJbiContainerBuilder extends AbstractDigesterConfiguration implements JbiContainerBuilder {
    //todo schema support
    public static final String DEFAULT_MULE_JBI_DTD = "mule-jbi-configuration.dtd";

    public static final String INBOUND_MESSAGE_ROUTER_INTERFACE = UMOInboundMessageRouter.class.getName();
    public static final String RESPONSE_MESSAGE_ROUTER_INTERFACE = UMOResponseMessageRouter.class.getName();
    public static final String OUTBOUND_MESSAGE_ROUTER_INTERFACE = UMOOutboundMessageRouter.class.getName();

    public static final String DEFAULT_OUTBOUND_MESSAGE_ROUTER = OutboundMessageRouter.class.getName();
    public static final String DEFAULT_INBOUND_MESSAGE_ROUTER = InboundMessageRouter.class.getName();
    public static final String DEFAULT_RESPONSE_MESSAGE_ROUTER = ResponseMessageRouter.class.getName();
    public static final String DEFAULT_CATCH_ALL_STRATEGY = LoggingCatchAllStrategy.class.getName();

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    protected JbiContainer container;
    protected List containerReferences = new ArrayList();

    public MuleXmlJbiContainerBuilder() {
        super(System.getProperty("org.mule.xml.validate", "false").equalsIgnoreCase("true"),
                System.getProperty("org.mule.xml.dtd", DEFAULT_MULE_JBI_DTD));


        ConvertUtils.register(new QNameConverter(digester), QName.class);
        ConvertUtils.register(new EndpointConverter(), UMOEndpoint.class);
        ConvertUtils.register(new TransformerConverter(), UMOTransformer.class);
        ConvertUtils.register(new ConnectorConverter(), UMOConnector.class);
        ConvertUtils.register(new TransactionFactoryConverter(), UMOTransactionFactory.class);
        ConvertUtils.register(new EndpointURIConverter(), UMOEndpointURI.class);

        try {
            String path = getRootName();
            addJbiContainerRules(digester, path);
            addServerPropertiesRules(path + "/properties", "setProperties", 0);
            addContainerContextRules(path + "/object-container", "addObjectContainer", 0);
            addMuleComponentRules(digester, path + "/mule-component");

        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    public String getRootName() {
        return "jbi-container";
    }

    public JbiContainer configure(String configResources) throws ConfigurationException {
        return configure(parseResources(configResources));
    }

    public JbiContainer configure(ReaderResource[] configResources) throws ConfigurationException {
        container = (JbiContainer) process(configResources);
        try {
            container.start();
        } catch (JBIException e) {
            e.printStackTrace();
        }
        return container;
    }

    /**
     * Indicate whether this ConfigurationBulder has been configured yet
     *
     * @return <code>true</code> if this ConfigurationBulder has been
     *         configured
     */
    public boolean isConfigured() {
        return container != null;
    }


    protected void addJbiContainerRules(Digester digester, String path) throws ConfigurationException {
        digester.addObjectCreate(path, JbiContainerImpl.class.getName());
        digester.addRule(path, new Rule(){
            public void begin(String s, String s1, Attributes attributes) throws Exception {
                ((JbiContainer)digester.getRoot()).initialize();
            }
        });
    }

    protected void setContainerProperties() throws ContainerException {
        UMOContainerContext ctx = container.getObjectContainer();
        try {
            for (Iterator iterator = containerReferences.iterator(); iterator.hasNext();) {
                ContainerReference reference = (ContainerReference) iterator.next();
                reference.resolveReference(ctx);
            }
        } finally {
            containerReferences.clear();
        }
    }

    protected void addMuleComponentRules(Digester digester, String path) throws ConfigurationException {
        // Create Mule Components
        //digester.addObjectCreate(path, ComponentDescriptor.class);
        digester.addRule(path, new ObjectGetOrCreateRule(path, null, "className", "ref", true, "getObjectContainer"));
 //{
//            public void begin(String s, String s1, Attributes attributes) throws Exception {
//                super.begin(s, s1, attributes);
//                Object o = digester.pop();
//                ComponentDescriptor cd = new ComponentDescriptor();
//                cd.setComponent((Component)o);
//                String name = attributes.getValue("name");
//                cd.setName(name);
//                digester.push(cd);
//            }
//        });
        //addObjectCreateOrGetFromContainer(path, null, "className", "ref", true);

        addSetPropertiesRule(path, digester);
        addMulePropertiesRule(path, digester);

        // Create Message Routers
        addMessageRouterRules(digester, path, "inbound");
        addMessageRouterRules(digester, path, "outbound");
        addMessageRouterRules(digester, path, "response");

        digester.addRule(path, new Rule(){
            public void end(String s, String s1) throws Exception {
                AbstractComponent c = (AbstractComponent)digester.peek();
                JbiContainer cont = ((JbiContainer)digester.getRoot());
                c.setContainer(cont);
                cont.getRegistry().addTransientEngine(c.getName(), c, c.getBootstrap());
            }
        });
    }


    protected void addMessageRouterRules(Digester digester, String path, String type) throws ConfigurationException {
        String defaultRouter = null;
        String setMethod = null;
        if ("inbound".equals(type)) {
            defaultRouter = DEFAULT_INBOUND_MESSAGE_ROUTER;
            setMethod = "setInboundRouter";
            path += "/inbound-router";
            // Add endpoints for multiple inbound endpoints
            addEndpointRules(digester, path, "addEndpoint");

        } else if ("response".equals(type)) {
            defaultRouter = DEFAULT_RESPONSE_MESSAGE_ROUTER;
            setMethod = "setResponseRouter";
            path += "/response-router";
            // Add endpoints for multiple response endpoints i.e. replyTo
            // addresses
            addEndpointRules(digester, path, "addEndpoint");
        } else {
            defaultRouter = DEFAULT_OUTBOUND_MESSAGE_ROUTER;
            setMethod = "setOutboundRouter";
            path += "/outbound-router";
        }
        digester.addObjectCreate(path, defaultRouter, "className");
        addSetPropertiesRule(path, digester);

        // Add Catch All strategy
        digester.addObjectCreate(path + "/catch-all-strategy", DEFAULT_CATCH_ALL_STRATEGY, "className");
        addSetPropertiesRule(path + "/catch-all-strategy", digester);

        // Add endpointUri for catch-all strategy
        addEndpointRules(digester, path + "/catch-all-strategy", "setEndpoint");

        addMulePropertiesRule(path + "/catch-all-strategy", digester);
        digester.addSetNext(path + "/catch-all-strategy", "setCatchAllStrategy");

        // Add router rules
        addRouterRules(digester, path, type);

        // add the router to the descriptor
        digester.addSetNext(path, setMethod);
    }

    protected void addCommonEndpointRules(Digester digester, String path, String method) throws ConfigurationException {
        addSetPropertiesRule(path,
                digester,
                new String[]{"address", "transformers", "createConnector"},
                new String[]{"endpointURI", "transformer", "createConnectorAsString"});

        addMulePropertiesRule(path, digester, "setProperties");
        //addTransactionConfigRules(path, digester);

        addFilterRules(digester, path);
        if (method != null) {
            digester.addSetNext(path, method);
        }

        // Add security filter rules
        //digester.addObjectCreate(path + "/security-filter", ENDPOINT_SECURITY_FILTER_INTERFACE, "className");

        //addMulePropertiesRule(path + "/security-filter", digester);
        //digester.addSetNext(path + "/security-filter", "setSecurityFilter");
    }

    protected void addRouterRules(Digester digester, String path, final String type) throws ConfigurationException {
        path += "/router";
        if ("inbound".equals(type)) {
            digester.addObjectCreate(path, INBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        } else if ("response".equals(type)) {
            digester.addObjectCreate(path, RESPONSE_MESSAGE_ROUTER_INTERFACE, "className");
        } else {
            digester.addObjectCreate(path, OUTBOUND_MESSAGE_ROUTER_INTERFACE, "className");
        }

        addSetPropertiesRule(path,
                digester,
                new String[]{"enableCorrelation"},
                new String[]{"enableCorrelationAsString"});
        addMulePropertiesRule(path, digester);
        if ("outbound".equals(type)) {
            addEndpointRules(digester, path, "addEndpoint");
            //addTransactionConfigRules(path, digester);
        }
        addFilterRules(digester, path);

        // Set the router on the to the message router
        digester.addSetNext(path, "addRouter");
    }

    protected void addEndpointRules(Digester digester, String path, String method) throws ConfigurationException {
        // Set message endpoint
        path += "/endpoint";
        addObjectCreateOrGetFromContainer(path, MuleEndpoint.class.getName(), "className", "ref", false);
        addCommonEndpointRules(digester, path, method);
    }

    protected void addObjectCreateOrGetFromContainer(final String path, String defaultImpl, final String classAttrib,
                                                     final String refAttrib, final boolean classRefRequired) {
        digester.addRule(path, new ObjectGetOrCreateRule(defaultImpl, classAttrib, refAttrib, classAttrib,
                classRefRequired, "getObjectContainer"));
    }
}
