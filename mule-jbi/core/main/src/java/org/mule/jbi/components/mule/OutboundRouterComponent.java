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
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.impl.MuleSession;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * IS a component that exposes Mule inbound routing functionality to Jbi components such as Idempotent
 * message receiving and response correlation
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OutboundRouterComponent extends AbstractComponent implements MessageListener
{
    private OutboundMessageRouter router;
    private JbiContainer container;

    public JbiContainer getContainer() {
        return container;
    }

    public void setContainer(JbiContainer container) {
        this.container = container;
    }


    public OutboundMessageRouter getRouter() {
        return router;
    }

    public void setRouter(OutboundMessageRouter router) {
        this.router = router;
    }

    protected void doInit() throws Exception {
        if(router==null) {
            throw new NullPointerException("Inbound Message router must be set");
        }


        QName routerService = new QName(getName());
        for (Iterator iterator = router.getRouters().iterator(); iterator.hasNext();) {
            UMOOutboundRouter r = (UMOOutboundRouter) iterator.next();

            for (Iterator iterator1 = r.getEndpoints().iterator(); iterator1.hasNext();) {
                UMOEndpoint endpoint = (UMOEndpoint) iterator1.next();
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
                    container.getRegistry().addTransientEngine(receiverComponent.getName(), receiverComponent, receiverComponent.getBootstrap());
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
            UMOMessage m = router.route(event.getMessage(), event.getSession(), event.isSynchronous());
            done(me);
        } catch (org.mule.umo.MessagingException e) {
            error(me, e);
        }
    }
}
