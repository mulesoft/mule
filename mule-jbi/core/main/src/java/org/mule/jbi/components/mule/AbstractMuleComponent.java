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

import org.mule.jbi.components.AbstractComponent;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.routing.response.ResponseMessageRouter;
import org.mule.registry.ComponentType;
import org.mule.registry.RegistryException;

import javax.jbi.JBIException;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMuleComponent extends AbstractComponent
 {
    private InboundMessageRouter inboundMessageRouter;
    private OutboundMessageRouter outboundMessageRouter;
    private ResponseMessageRouter responseMessageRouter;


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

    protected void doInit() throws Exception {
        registerInboundRouter();
        registerOutboundRouter();
        registerResponseRouter();
    }

    protected void registerInboundRouter() throws JBIException, IOException {
        if (inboundMessageRouter == null) return;
        InboundRouterComponent routerComponent = new InboundRouterComponent();
        routerComponent.setRouter(inboundMessageRouter);
        routerComponent.setName("inboundRouter:" + getName());
        routerComponent.setContainer(container);
        QName targetService = new QName(getName());
        routerComponent.setTargetService(targetService);
        try {
            container.getRegistry().addTransientComponent(routerComponent.getName(),
                    ComponentType.JBI_ENGINE_COMPONENT, routerComponent,
                    routerComponent.getBootstrap());
        } catch (RegistryException e) {
            throw new JBIException(e);
        }
        getContext().activateEndpoint(targetService, targetService.getLocalPart());
    }

     protected void registerOutboundRouter() throws JBIException, IOException {
        if (outboundMessageRouter == null) return;
     }

    protected void registerResponseRouter() throws JBIException, IOException {
        if (responseMessageRouter == null) return;
     }
}
