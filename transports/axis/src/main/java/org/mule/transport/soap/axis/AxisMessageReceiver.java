/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.component.AbstractJavaComponent;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.soap.axis.extensions.MuleMsgProvider;
import org.mule.transport.soap.axis.extensions.MuleRPCProvider;
import org.mule.transport.soap.axis.i18n.AxisMessages;
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

    public static final String AXIS_OPTIONS = "axisOptions";
    public static final String BEAN_TYPES = "beanTypes";
    public static final String SERVICE_NAMESPACE = "serviceNamespace";

    protected AxisConnector connector;
    protected SOAPService soapService;

    public AxisMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {

        super(connector, flowConstruct, endpoint);

        this.connector = (AxisConnector) connector;
        try
        {
            AxisServiceProxy.setProperties(endpoint.getProperties());
            create();
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
        }
    }

    protected void create() throws Exception
    {
        if (!(flowConstruct instanceof Service))
        {
            throw new IllegalArgumentException(
                "Only the Service flow constuct is supported by the axis transport");
        }
        Service service = (Service) flowConstruct;


        AxisProperties.setProperty("axis.doAutoTypes", String.valueOf(connector.isDoAutoTypes()));
        String style = (String) endpoint.getProperties().get(AxisConnector.STYLE);
        String use = (String) endpoint.getProperties().get(AxisConnector.USE);
        String doc = (String) endpoint.getProperties().get("documentation");

        EndpointURI uri = endpoint.getEndpointURI();
        String serviceName = flowConstruct.getName();

        SOAPService existing = this.connector.getAxis().getService(serviceName);
        if (existing != null)
        {
            soapService = existing;
            logger.debug("Using existing service for " + serviceName);
        }
        else
        {
            // Check if the style is message. If so, we need to create
            // a message oriented provider
            if (style != null && style.equalsIgnoreCase("message"))
            {
                logger.debug("Creating Message Provider");
                soapService = new SOAPService(new MuleMsgProvider(connector));
                // } else if (style != null && style.equalsIgnoreCase("document")) {
                // logger.debug("Creating Doc Provider");
                // service = new SOAPService(new MuleDocLitProvider(connector));
            }
            else
            {
                logger.debug("Creating RPC Provider");
                soapService = new SOAPService(new MuleRPCProvider(connector));
            }

            soapService.setEngine(connector.getAxis());
        }

        String servicePath = uri.getPath();
        soapService.setOption(serviceName, this);
        soapService.setOption(AxisConnector.SERVICE_PROPERTY_SERVCE_PATH, servicePath);
        soapService.setOption(AxisConnector.SERVICE_PROPERTY_COMPONENT_NAME, serviceName);

        soapService.setName(serviceName);

        // Add any custom options from the Descriptor config
        Map options = (Map) endpoint.getProperties().get(AXIS_OPTIONS);

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
            soapService.setOption(entry.getKey().toString(), entry.getValue());
            logger.debug("Adding Axis option: " + entry);
        }

        // set method names
        Class[] interfaces = AxisServiceProxy.getInterfacesForComponent(service);
        if (interfaces.length == 0)
        {
            throw new InitialisationException(
                    AxisMessages.objectMustImplementAnInterface(serviceName), service);
        }
        // You must supply a class name if you want to restrict methods
        // or specify the 'allowedMethods' property in the axisOptions property
        String methodNames = "*";

        Map<?, ?> methods = (Map<?, ?>) endpoint.getProperties().get(AxisConnector.SOAP_METHODS);
        if (methods != null)
        {
            Iterator<?> i = methods.keySet().iterator();
            StringBuilder buf = new StringBuilder(64);
            while (i.hasNext())
            {
                String name = (String) i.next();
                Object m = methods.get(name);
                SoapMethod method;
                if (m instanceof List)
                {
                    method = new SoapMethod(name, (List<String>) m);
                }
                else
                {
                    method = new SoapMethod(name, (String) m);
                }

                List<?> namedParameters = method.getNamedParameters();
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

                soapService.getServiceDescription().addOperationDesc(
                        new OperationDesc(method.getName().getLocalPart(), parameters, method.getReturnType()));
                buf.append(method.getName().getLocalPart() + ",");
            }
            methodNames = buf.toString();
            methodNames = methodNames.substring(0, methodNames.length() - 1);
        }
        else
        {
            String[] methodNamesArray = AxisServiceProxy.getMethodNames(interfaces);
            StringBuilder buf = new StringBuilder(64);
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
        String namespace = (String) endpoint.getProperties().get(SERVICE_NAMESPACE);
        if (namespace == null)
        {
            namespace = Namespaces.makeNamespace(className);
        }

        // WSDL override
        String wsdlFile = (String) endpoint.getProperties().get("wsdlFile");
        if (wsdlFile != null)
        {
            soapService.getServiceDescription().setWSDLFile(wsdlFile);
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
        setOptionIfNotset(soapService, JavaProvider.OPTION_WSDL_SERVICEPORT, serviceName);
        setOptionIfNotset(soapService, JavaProvider.OPTION_CLASSNAME, className);
        setOptionIfNotset(soapService, JavaProvider.OPTION_SCOPE, "Request");
        if (StringUtils.isNotBlank(namespace))
        {
            setOptionIfNotset(soapService, JavaProvider.OPTION_WSDL_TARGETNAMESPACE, namespace);
        }

        // Set the allowed methods, allow all if there are none specified.
        if (methodNames == null)
        {
            setOptionIfNotset(soapService, JavaProvider.OPTION_ALLOWEDMETHODS, "*");
        }
        else
        {
            setOptionIfNotset(soapService, JavaProvider.OPTION_ALLOWEDMETHODS, methodNames);
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
                        CoreMessages.valueIsInvalidFor(style, AxisConnector.STYLE), this);
            }
            else
            {
                soapService.setStyle(s);
            }
        }
        // Set use: Endcoded/Literal
        if (use != null)
        {
            Use u = Use.getUse(use);
            if (u == null)
            {
                throw new CreateException(CoreMessages.valueIsInvalidFor(use, AxisConnector.USE),
                        this);
            }
            else
            {
                soapService.setUse(u);
            }
        }

        soapService.getServiceDescription().setDocumentation(doc);

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
        // List types = (List) descriptor.getProperties().get(BEAN_TYPES);
        // connector.registerTypes(registry, types);

        soapService.setName(serviceName);

        // Add initialisation callback for the Axis service
        Component component = service.getComponent();
        if (component instanceof JavaComponent)
        {
            ((AbstractJavaComponent) component).getObjectFactory().addObjectInitialisationCallback(
                new AxisInitialisationCallback(soapService));
        }

        if (uri.getScheme().equalsIgnoreCase("servlet"))
        {
            connector.addServletService(soapService);
            String endpointUrl = uri.getAddress() + "/" + serviceName;
            endpointUrl = endpointUrl.replaceFirst("servlet:", "http:");
            soapService.getServiceDescription().setEndpointURL(endpointUrl);
        }
        else
        {
            soapService.getServiceDescription().setEndpointURL(uri.getAddress() + "/" + serviceName);
        }
        if (StringUtils.isNotBlank(namespace))
        {
            soapService.getServiceDescription().setDefaultNamespace(namespace);
        }
        soapService.init();
        soapService.stop();
    }

    @Override
    protected void doConnect() throws Exception
    {
        // Tell the axis configuration about our new service.
        connector.getServerProvider().deployService(soapService.getName(), soapService);
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        try
        {
            doStop();
        }
        catch (MuleException e)
        {
            logger.error(e.getMessage(), e);
        }
        // TODO: how do you undeploy an Axis service?

        // Unregister the mule part of the service
        connector.unregisterReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    @Override
    protected void doStart() throws MuleException
    {
        if (soapService != null)
        {
            soapService.start();
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();

        if (soapService != null)
        {
            soapService.stop();
        }
    }

    @Override
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

    public SOAPService getSoapService()
    {
        return soapService;
    }
}
