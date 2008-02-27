/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.config.spring.editors.QNamePropertyEditor;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.soap.SoapConstants;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.codehaus.xfire.annotations.WebAnnotations;
import org.codehaus.xfire.annotations.WebServiceAnnotation;
import org.codehaus.xfire.handler.Handler;
import org.codehaus.xfire.service.Service;

/**
 * Used to register an Xfire endpoint registered with Mule and associated with a
 * service This receiver is responsible or registering the transport endpoint i.e.
 * http:// as well as managing the association of this transport endpoint with the
 * Xfire service.
 */
public class XFireMessageReceiver extends AbstractMessageReceiver
{
    private static final String PORT_TYPE = "portType";

    protected XFireConnector connector;
    protected Service xfireService;

    protected List serviceInterfaces;

    public XFireMessageReceiver(Connector umoConnector, org.mule.api.service.Service service, InboundEndpoint umoEndpoint)
        throws CreateException
    {
        super(umoConnector, service, umoEndpoint);

        connector = (XFireConnector) umoConnector;
        create();
    }

    protected void create() throws CreateException
    {
        try
        {
            Map props = new HashMap(endpoint.getProperties());

            // convert port Type to QName if specified
            if (props.containsKey(PORT_TYPE))
            {
                Object value = props.get(PORT_TYPE);
                QName portTypeQName = QNamePropertyEditor.convert((String) value);
                props.put(PORT_TYPE, portTypeQName);
            }

            // check if there is the namespace property on the service
            String namespace = (String) endpoint.getProperties().get(SoapConstants.SOAP_NAMESPACE_PROPERTY);

            // check for namespace set as annotation
            if (connector.isEnableJSR181Annotations())
            {
                WebAnnotations wa = (WebAnnotations) ClassUtils.instanciateClass(
                    XFireConnector.CLASSNAME_ANNOTATIONS, null, this.getClass());
                // at this point, the object hasn't been created in the descriptor so
                // we have to retrieve the implementation classname and create a
                // class for it
                WebServiceAnnotation webServiceAnnotation = 
                    wa.getWebServiceAnnotation(service.getServiceFactory().getObjectClass());
                namespace = webServiceAnnotation.getTargetNamespace();
            }

            if ((namespace == null) || (namespace.equalsIgnoreCase("")))
            {
                namespace = MapUtils.getString(props, "namespace", XFireConnector.DEFAULT_MULE_NAMESPACE_URI);
            }

            //Convert createDefaultBindings string to boolean before rewriting as xfire property
            if (props.get("createDefaultBindings") != null)
            {
                props.put("createDefaultBindings", Boolean.valueOf((String) props.get("createDefaultBindings")));
            }

            if (props.size() == 0)
            {
                // Xfire checks that properties are null rather than empty
                props = null;
            }
            else
            {
                rewriteProperty(props, PORT_TYPE);
                rewriteProperty(props, "style");
                rewriteProperty(props, "use");
                rewriteProperty(props, "createDefaultBindings");
                rewriteProperty(props, "soap12Transports");
                rewriteProperty(props, "soap11Transports");
                rewriteProperty(props, "scope");
                rewriteProperty(props, "schemas");
            }

            serviceInterfaces = (List)endpoint.getProperties().get(SoapConstants.SERVICE_INTERFACES);
            Class exposedInterface;

            if (serviceInterfaces == null)
            {
                exposedInterface = service.getServiceFactory().getObjectClass();
            }
            else
            {
                String className = (String) serviceInterfaces.get(0);
                exposedInterface = ClassUtils.loadClass(className, this.getClass());
                logger.info(className + " class was used to expose your service");

                if (serviceInterfaces.size() > 1)
                {
                    logger.info("Only the first class was used to expose your method");
                }
            }

            String wsdlUrl = (String) endpoint.getProperties().get(SoapConstants.WSDL_URL_PROPERTY);

            if (StringUtils.isBlank(wsdlUrl))
            {
                xfireService = connector.getServiceFactory().create(exposedInterface,
                    service.getName(), namespace, props);
            }
            else
            {
                xfireService = connector.getServiceFactory().create(exposedInterface,
                    new QName(namespace, service.getName()), new URL(wsdlUrl), props);
            }
            
            List inList = connector.getServerInHandlers();
            if (inList != null)
            {
                for (int i = 0; i < inList.size(); i++)
                {
                    Class clazz = ClassUtils.loadClass(inList.get(i).toString(), this.getClass());
                    Handler handler = (Handler) clazz.getConstructor((Class[])null).newInstance((Object[])null);
                    xfireService.addInHandler(handler);
                }
            }

            boolean sync = endpoint.isSynchronous();
            xfireService.setInvoker(new MuleInvoker(this, sync));
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
        }
    }

    protected void doDispose()
    {
        // template method
    }

    public void doConnect() throws Exception
    {
        // Tell the Xfire registry about our new service.
        connector.getXfire().getServiceRegistry().register(xfireService);
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    public void doDisconnect() throws Exception
    {
        connector.getXfire().getServiceRegistry().unregister(xfireService);
    }

    public void doStart() throws MuleException
    {
        // nothing to do
    }

    public void doStop() throws MuleException
    {
        // nothing to do
    }

    protected void rewriteProperty(Map props, String name)
    {
        if (props.containsKey(name))
        {
            Object temp = props.remove(name);
            props.put("objectServiceFactory." + name, temp);
        }
    }

}
