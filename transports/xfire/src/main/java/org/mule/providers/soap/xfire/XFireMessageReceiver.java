/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.config.converters.QNameConverter;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.soap.SoapConstants;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ClassUtils;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;
import org.mule.util.object.SimpleObjectFactory;

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
 * component This receiver is responsible or registering the transport endpoint i.e.
 * http:// as well as managing the association of this transport endpoint with the
 * Xfire service.
 */
public class XFireMessageReceiver extends AbstractMessageReceiver
{
    private static final String PORT_TYPE = "portType";

    protected XFireConnector connector;
    protected Service service;

    protected List serviceInterfaces;

    public XFireMessageReceiver(UMOConnector umoConnector, UMOComponent component, UMOEndpoint umoEndpoint)
        throws CreateException
    {
        super(umoConnector, component, umoEndpoint);

        connector = (XFireConnector) umoConnector;
        create();
    }

    protected void create() throws CreateException
    {
        try
        {
            Map props = new HashMap(component.getProperties());
            props.putAll(endpoint.getProperties());

            // convert port Type to QName if specified
            if (props.containsKey(PORT_TYPE))
            {
                Object value = props.get(PORT_TYPE);
                QNameConverter converter = new QNameConverter(true);
                QName portTypeQName = (QName) converter.convert(QName.class, value);
                props.put(PORT_TYPE, portTypeQName);
            }

            // check if there is the namespace property on the component
            String namespace = (String) component.getProperties().get(SoapConstants.SOAP_NAMESPACE_PROPERTY);

            // check for namespace set as annotation
            if (connector.isEnableJSR181Annotations())
            {
                WebAnnotations wa = (WebAnnotations) ClassUtils.instanciateClass(
                    XFireConnector.CLASSNAME_ANNOTATIONS, null, this.getClass());
                // at this point, the object hasn't been created in the descriptor so
                // we have to retrieve the implementation classname and create a
                // class for it
                WebServiceAnnotation webServiceAnnotation = 
                    wa.getWebServiceAnnotation(Class.forName(
                        ((SimpleObjectFactory)component.getServiceFactory()).getObjectClassName()));
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

            serviceInterfaces = (List)component.getProperties().get("serviceInterfaces");
            Class exposedInterface;

            if (serviceInterfaces == null)
            {
                exposedInterface = component.getServiceFactory().getOrCreate().getClass();
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

            String wsdlUrl = (String) component.getProperties().get(SoapConstants.WSDL_URL_PROPERTY);

            if (StringUtils.isBlank(wsdlUrl))
            {
                service = connector.getServiceFactory().create(exposedInterface,
                    component.getName(), namespace, props);
            }
            else
            {
                service = connector.getServiceFactory().create(exposedInterface,
                    new QName(namespace, component.getName()), new URL(wsdlUrl), props);
            }

            List inList = connector.getServerInHandlers();
            if (inList != null)
            {
                for (int i = 0; i < inList.size(); i++)
                {
                    Class clazz = ClassUtils.loadClass(inList.get(i).toString(), this.getClass());
                    Handler handler = (Handler) clazz.getConstructor(null).newInstance(null);
                    service.addInHandler(handler);
                }
            }

            boolean sync = endpoint.isSynchronous();
            service.setInvoker(new MuleInvoker(this, sync));
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
        connector.getXfire().getServiceRegistry().register(service);
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    public void doDisconnect() throws Exception
    {
        connector.getXfire().getServiceRegistry().unregister(service);
    }

    public void doStart() throws UMOException
    {
        // nothing to do
    }

    public void doStop() throws UMOException
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
