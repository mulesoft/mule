/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleDescriptor;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.ServiceProxy;
import org.mule.providers.soap.SoapMethod;
import org.mule.providers.soap.axis.extensions.MuleMsgProvider;
import org.mule.providers.soap.axis.extensions.MuleRPCProvider;
import org.mule.providers.soap.axis.i18n.AxisMessages;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.AxisProperties;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.providers.java.JavaProvider;
import org.apache.axis.wsdl.fromJava.Namespaces;

/**
 * <code>AxisMessageReceiver</code> is used to register a component as a service
 * with a Axis server.
 */

public class AxisMessageReceiver extends AbstractMessageReceiver
{
    protected AxisConnector connector;
    protected SOAPService service;

    public AxisMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws CreateException
    {

        super(connector, component, endpoint);

        this.connector = (AxisConnector) connector;
        try
        {
            create();
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
        }
    }

    protected void create() throws Exception
    {
        AxisProperties.setProperty("axis.doAutoTypes", String.valueOf(connector.isDoAutoTypes()));
        MuleDescriptor descriptor = (MuleDescriptor) component.getDescriptor();
        // TODO RM: these are endpoint properties now
//        String style = (String)descriptor.getProperties().get("style");
//        String use = (String)descriptor.getProperties().get("use");
//        String doc = (String)descriptor.getProperties().get("documentation");
        String style = (String) endpoint.getProperties().get("style");
        String use = (String) endpoint.getProperties().get("use");
        String doc = (String) endpoint.getProperties().get("documentation");

        UMOEndpointURI uri = endpoint.getEndpointURI();
        String serviceName = component.getDescriptor().getName();

        SOAPService existing = this.connector.getAxisServer().getService(serviceName);
        if (existing != null)
        {
            service = existing;
            logger.debug("Using existing service for " + serviceName);
        }
        else
        {
            // Check if the style is message. If so, we need to create
            // a message oriented provider
            if (style != null && style.equalsIgnoreCase("message"))
            {
                logger.debug("Creating Message Provider");
                service = new SOAPService(new MuleMsgProvider(connector));
                // } else if (style != null && style.equalsIgnoreCase("document")) {
                // logger.debug("Creating Doc Provider");
                // service = new SOAPService(new MuleDocLitProvider(connector));
            }
            else
            {
                logger.debug("Creating RPC Provider");
                service = new SOAPService(new MuleRPCProvider(connector));
            }

            service.setEngine(connector.getAxisServer());
        }

        String servicePath = uri.getPath();
        service.setOption(serviceName, this);
        service.setOption(AxisConnector.SERVICE_PROPERTY_SERVCE_PATH, servicePath);
        service.setOption(AxisConnector.SERVICE_PROPERTY_COMPONENT_NAME, serviceName);

        service.setName(serviceName);

        // Add any custom options from the Descriptor config
        Map options = (Map) endpoint.getProperties().get("axisOptions");

        // IF wsdl service name is not set, default to service name
        if (options == null)
        {
            options = new HashMap(2);
        }
        if (options.get("wsdlServiceElement") == null)
        {
            options.put("wsdlServiceElement", serviceName);
        }

        Map.Entry entry;
        for (Iterator iterator = options.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry) iterator.next();
            service.setOption(entry.getKey().toString(), entry.getValue());
            logger.debug("Adding Axis option: " + entry);
        }

        // set method names
        Class[] interfaces = ServiceProxy.getInterfacesForComponent(component);
        if (interfaces.length == 0)
        {
            throw new InitialisationException(
                    AxisMessages.objectMustImplementAnInterface(serviceName), component);
        }
        // You must supply a class name if you want to restrict methods
        // or specify the 'allowedMethods' property in the axisOptions property
        String methodNames = "*";

        Map methods = (Map) endpoint.getProperties().get("soapMethods");
        if (methods == null)
        {
            methods = (Map) descriptor.getProperties().get("soapMethods");
        }
        if (methods != null)
        {
            Iterator i = methods.keySet().iterator();
            StringBuffer buf = new StringBuffer(64);
            while (i.hasNext())
            {
                String name = (String) i.next();
                Object m = methods.get(name);
                SoapMethod method = null;
                if (m instanceof List)
                {
                    method = new SoapMethod(name, (List) m);
                }
                else
                {
                    method = new SoapMethod(name, (String) m);
                }

                List namedParameters = method.getNamedParameters();
                ParameterDesc[] parameters = new ParameterDesc[namedParameters.size()];
                for (int j = 0; j < namedParameters.size(); j++)
                {
                    NamedParameter parameter = (NamedParameter) namedParameters.get(j);
                    byte mode = ParameterDesc.INOUT;
                    if (parameter.getMode().equals(ParameterMode.IN))
                    {
                        mode = ParameterDesc.IN;
                    }
                    else if (parameter.getMode().equals(ParameterMode.OUT))
                    {
                        mode = ParameterDesc.OUT;
                    }

                    parameters[j] = new ParameterDesc(parameter.getName(), mode, parameter.getType());
                }

                service.getServiceDescription().addOperationDesc(
                        new OperationDesc(method.getName().getLocalPart(), parameters, method.getReturnType()));
                buf.append(method.getName().getLocalPart() + ",");
            }
            methodNames = buf.toString();
            methodNames = methodNames.substring(0, methodNames.length() - 1);
        }
        else
        {
            String[] methodNamesArray = ServiceProxy.getMethodNames(interfaces);
            StringBuffer buf = new StringBuffer(64);
            for (int i = 0; i < methodNamesArray.length; i++)
            {
                buf.append(methodNamesArray[i]).append(",");
            }
            methodNames = buf.toString();
            methodNames = methodNames.substring(0, methodNames.length() - 1);
        }

        String className = interfaces[0].getName();
        // The namespace of the service.
        // Todo use the service qname in Mule 2.0
        // TODO RM: this is an endpoint property now
        // String namespace = (String)descriptor.getProperties().get("serviceNamespace");
        String namespace = (String) endpoint.getProperties().get("serviceNamespace");
        if (namespace == null)
        {
            namespace = Namespaces.makeNamespace(className);
        }

        // WSDL override
        // TODO RM: this is an endpoint property now?
        // String wsdlFile = (String)descriptor.getProperties().get("wsdlFile");
        String wsdlFile = (String) endpoint.getProperties().get("wsdlFile");
        if (wsdlFile != null)
        {
            service.getServiceDescription().setWSDLFile(wsdlFile);
        }
        /*
         * Now we set up the various options for the SOAPService. We set:
         * RPCProvider.OPTION_WSDL_SERVICEPORT In essense, this is our service name
         * RPCProvider.OPTION_CLASSNAME This tells the serverProvider (whether it be
         * an AvalonProvider or just JavaProvider) what class to load via
         * "makeNewServiceObject". RPCProvider.OPTION_SCOPE How long the object
         * loaded via "makeNewServiceObject" will persist - either request, session,
         * or application. We use the default for now.
         * RPCProvider.OPTION_WSDL_TARGETNAMESPACE A namespace created from the
         * package name of the service. RPCProvider.OPTION_ALLOWEDMETHODS What
         * methods the service can execute on our class. We don't set:
         * RPCProvider.OPTION_WSDL_PORTTYPE RPCProvider.OPTION_WSDL_SERVICEELEMENT
         */
        setOptionIfNotset(service, JavaProvider.OPTION_WSDL_SERVICEPORT, serviceName);
        setOptionIfNotset(service, JavaProvider.OPTION_CLASSNAME, className);
        setOptionIfNotset(service, JavaProvider.OPTION_SCOPE, "Request");
        if (StringUtils.isNotBlank(namespace))
        {
            setOptionIfNotset(service, JavaProvider.OPTION_WSDL_TARGETNAMESPACE, namespace);
        }

        // Set the allowed methods, allow all if there are none specified.
        if (methodNames == null)
        {
            setOptionIfNotset(service, JavaProvider.OPTION_ALLOWEDMETHODS, "*");
        }
        else
        {
            setOptionIfNotset(service, JavaProvider.OPTION_ALLOWEDMETHODS, methodNames);
        }

        // Note that Axis has specific rules to how these two variables are
        // combined. This is handled for us
        // Set style: RPC/wrapped/Doc/Message

        if (style != null)
        {
            Style s = Style.getStyle(style);
            if (s == null)
            {
                throw new CreateException(
                        CoreMessages.valueIsInvalidFor(style, "style"), this);
            }
            else
            {
                service.setStyle(s);
            }
        }
        // Set use: Endcoded/Literal
        if (use != null)
        {
            Use u = Use.getUse(use);
            if (u == null)
            {
                throw new CreateException(CoreMessages.valueIsInvalidFor(use, "use"),
                        this);
            }
            else
            {
                service.setUse(u);
            }
        }

        service.getServiceDescription().setDocumentation(doc);

        // Tell Axis to try and be intelligent about serialization.
        // TypeMappingRegistryImpl registry = (TypeMappingRegistryImpl)
        // service.getTypeMappingRegistry();
        // TypeMappingImpl tm = (TypeMappingImpl) registry.();

        // Handle complex bean type automatically
        // registry.setDoAutoTypes( true );

        // Axis 1.2 fix to handle autotypes properly
        // AxisProperties.setProperty("axis.doAutoTypes",
        // String.valueOf(connector.isDoAutoTypes()));

        // TODO Load any explicitly defined bean types
        // List types = (List) descriptor.getProperties().get("beanTypes");
        // connector.registerTypes(registry, types);

        service.setName(serviceName);

        // Add initialisation callback for the Axis service
        descriptor.addInitialisationCallback(new AxisInitialisationCallback(service));

        if (uri.getScheme().equalsIgnoreCase("servlet"))
        {
            connector.addServletService(service);
            String endpointUrl = uri.getAddress() + "/" + serviceName;
            endpointUrl = endpointUrl.replaceFirst("servlet:", "http:");
            service.getServiceDescription().setEndpointURL(endpointUrl);
        }
        else
        {
            service.getServiceDescription().setEndpointURL(uri.getAddress() + "/" + serviceName);
        }
        if (StringUtils.isNotBlank(namespace))
        {
            service.getServiceDescription().setDefaultNamespace(namespace);
        }
        service.init();
        service.stop();
    }

    protected void doConnect() throws Exception
    {
        // Tell the axis configuration about our new service.
        connector.getServerProvider().deployService(service.getName(), service);
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    protected void doDisconnect() throws Exception
    {
        try
        {
            doStop();
        }
        catch (UMOException e)
        {
            logger.error(e.getMessage(), e);
        }
        // TODO: how do you undeploy an Axis service?

        // Unregister the mule part of the service
        connector.unregisterReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    protected void doStart() throws UMOException
    {
        if (service != null)
        {
            service.start();
        }
    }

    protected void doStop() throws UMOException
    {
        if (service != null)
        {
            service.stop();
        }
    }

    protected void doDispose()
    {
        // nothing to do               
    }

    protected void setOptionIfNotset(SOAPService service, String option, Object value)
    {
        Object val = service.getOption(option);
        if (val == null)
        {
            service.setOption(option, value);
        }
    }

    public SOAPService getService()
    {
        return service;
    }
}
