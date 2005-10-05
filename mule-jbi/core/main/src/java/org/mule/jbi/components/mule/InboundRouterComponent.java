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
package org.mule.jbi.components.mule;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.components.AbstractComponent;
import org.mule.jbi.messaging.MessageListener;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.registry.RegistryException;
import org.mule.registry.ComponentType;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.JBIException;
import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * IS a component that exposes Mule inbound routing functionality to Jbi components such as Idempotent
 * message receiving and response correlation
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class InboundRouterComponent extends AbstractComponent implements MessageListener
{
    private InboundMessageRouter router;
    private QName targetService;
    private JbiContainer container;

    public JbiContainer getContainer() {
        return container;
    }

    public void setContainer(JbiContainer container) {
        this.container = container;
    }

    public QName getTargetService() {
        return targetService;
    }

    public void setTargetService(QName targetService) {
        this.targetService = targetService;
    }

    public InboundMessageRouter getRouter() {
        return router;
    }

    public void setRouter(InboundMessageRouter router) {
        this.router = router;
    }

    protected void doInit() throws Exception {
        if(router==null) {
            throw new NullPointerException("Inbound Message router must be set");
        }
        if(targetService==null) {
            throw new NullPointerException("Inbound Message router must be set");
        }

        QName routerService = new QName(getName());
        for (Iterator iterator = router.getEndpoints().iterator(); iterator.hasNext();) {
            UMOEndpoint endpoint = (UMOEndpoint) iterator.next();
            endpoint.initialise();
            if(endpoint.getEndpointURI().getScheme().equals("container")) {
                QName name = getServiceName(endpoint);
                context.activateEndpoint(name, endpoint.getEndpointURI().getAddress());
            } else {
                MuleReceiverComponent receiverComponent = new MuleReceiverComponent();
                receiverComponent.setEndpoint(endpoint);
                receiverComponent.setName(endpoint.getEndpointURI().getScheme() + ":" + getName());

                receiverComponent.setTargetService(routerService);
                receiverComponent.setContainer(container);

                try {
                    container.getRegistry().addTransientComponent(receiverComponent.getName(),
                            ComponentType.JBI_ENGINE_COMPONENT, receiverComponent,
                            receiverComponent.getBootstrap());
                } catch (RegistryException e) {
                    throw new JBIException(e);
                }
            }
        }
        getContext().activateEndpoint(routerService, routerService.getLocalPart());
    }

    protected QName getServiceName(UMOEndpoint endpoint)
    {
        String serviceName = (String)endpoint.getProperties().get("serviceName");
        String namespaceUri = (String)endpoint.getProperties().get("namespaceUri");
        String namespacePrefix = (String)endpoint.getProperties().get("namespacePrefix");

        if(namespaceUri==null) {
            return new QName(serviceName);
        }else if (namespacePrefix==null) {
            return new QName(namespaceUri, serviceName);
        } else {
            return new QName(namespaceUri, serviceName, namespacePrefix);
        }

    }

    public void onMessage(MessageExchange me) throws MessagingException {
        NormalizedMessage message = me.getMessage(IN);
        UMOEvent event = JbiUtils.createEvent(message, this);
        try {
            UMOMessage m = router.route(event);
            if(m!=null) {
                InOnly exchange = context.getDeliveryChannel().createExchangeFactory().createInOnlyExchange();
                NormalizedMessage nmessage = exchange.createMessage();

                ServiceEndpoint endpoint = null;
                ServiceEndpoint[] eps = context.getEndpointsForService(targetService);
                if(eps.length==0) {
                    //container should handle this
                    //throw new MessagingException("There are no endpoints registered for targetService: " + targetService);
                } else {
                    endpoint = eps[0];
                }

                if (endpoint != null) {
                    exchange.setEndpoint(endpoint);
                }

                exchange.setInMessage(nmessage);
                JbiUtils.populateNormalizedMessage(m, nmessage);
                boolean synchronous = m.getBooleanProperty("synchronous", true);
            if(synchronous) {
                //todo timeout
                getContext().getDeliveryChannel().sendSync(me);
            } else {
                getContext().getDeliveryChannel().send(me);
            }

            }
            done(me);
        } catch (org.mule.umo.MessagingException e) {
            error(me, e);
        }
    }
}
