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

import org.mule.jbi.JbiContainer;
import org.mule.jbi.components.AbstractComponent;
import org.mule.jbi.components.mule.InboundRouterComponent;
import org.mule.jbi.registry.Engine;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.response.ResponseMessageRouter;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentDescriptor {

    private String name;
    private Component component;
    private InboundMessageRouter inboundMessageRouter;
    private OutboundMessageRouter outboundMessageRouter;
    private ResponseMessageRouter responseMessageRouter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public InboundMessageRouter getInboundRouter() {
        return inboundMessageRouter;
    }

    public void setInboundRouter(InboundMessageRouter inboundMessageRouter) {
        this.inboundMessageRouter = inboundMessageRouter;
    }

    public OutboundMessageRouter getOutboundRouter() {
        return outboundMessageRouter;
    }

    public void setOutboundRouter(OutboundMessageRouter outboundMessageRouter) {
        this.outboundMessageRouter = outboundMessageRouter;
    }

    public ResponseMessageRouter getResponseRouter() {
        return responseMessageRouter;
    }

    public void setResponseRouter(ResponseMessageRouter responseMessageRouter) {
        this.responseMessageRouter = responseMessageRouter;
    }

    public void register(JbiContainer container) throws JBIException, IOException {
        Engine e = container.getRegistry().addTransientEngine(name, component);
        ComponentContext ctx = ((AbstractComponent)component).getContext();

        registerInboundRouter(container, ctx);
    }

    protected void registerInboundRouter(JbiContainer container, ComponentContext ctx) throws JBIException, IOException {
        if(inboundMessageRouter==null) return;
        InboundRouterComponent routerComponent = new InboundRouterComponent();
        routerComponent.setRouter(inboundMessageRouter);
        QName routerService = new QName(name + ":InboundRouter");
        routerComponent.setTargetService(routerService);
        container.getRegistry().addTransientEngine(routerService.getLocalPart(), routerComponent);
        routerComponent.getContext().activateEndpoint(routerService, routerService.getLocalPart());
    }

}
