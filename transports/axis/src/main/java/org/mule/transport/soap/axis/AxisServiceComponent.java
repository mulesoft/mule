/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.WriterMessageAdapter;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.soap.SoapConstants;
import org.mule.transport.soap.axis.extensions.AxisMuleSession;
import org.mule.transport.soap.axis.extensions.MuleConfigProvider;
import org.mule.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.ConfigurationException;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.i18n.Messages;
import org.apache.axis.security.servlet.ServletSecurityProvider;
import org.apache.axis.server.AxisServer;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.transport.http.ServletEndpointContextImpl;
import org.apache.axis.utils.Admin;
import org.apache.axis.utils.XMLUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

/**
 * <code>AxisServiceComponent</code> is a Mule component implementation of the Axis
 * servlet. This component supports all the features of the Axis servlet except -
 * <ol>
 * <li>Jws class services are not supported as they don't add any value to the Mule
 * model</li>
 * <li>Currently there is no HttpSession support. This will be fixed when MuleSession
 * support is added to the Http Connector</li>
 * </ol>
 */

public class AxisServiceComponent implements Initialisable, Callable
{
    protected static final Log logger = org.apache.commons.logging.LogFactory.getLog(AxisServiceComponent.class);

    public static final String INIT_PROPERTY_TRANSPORT_NAME = "transport.name";
    public static final String INIT_PROPERTY_USE_SECURITY = "use-servlet-security";
    public static final String INIT_PROPERTY_ENABLE_LIST = "axis.enableListQuery";
    public static final String DEFAULT_AXIS_HOME = "/axisHome";

    private String transportName = "http";
    private ServletSecurityProvider securityProvider = null;
    private boolean enableList = true;
    private String homeDir;
    private AxisServer axis;

    /** For IoC */
    public AxisServiceComponent()
    {
        // do nothing
    }

    /**
     * Passes the context to the listener
     * 
     * @param context the context to process
     * @return Object this object can be anything. When the
     *         <code>LifecycleAdapter</code> for the component receives this
     *         object it will first see if the Object is an <code>MuleEvent</code>
     *         if not and the Object is not null a new context will be created using
     *         the returned object as the payload. This new context will then get
     *         published to the configured outbound endpoint if-
     *         <ol>
     *         <li>One has been configured for the component.</li>
     *         <li>the <code>setStopFurtherProcessing(true)</code> wasn't called
     *         on the previous context.</li>
     *         </ol>
     * @throws Exception if the context fails to process properly. If exceptions
     *             aren't handled by the implementation they will be handled by the
     *             exceptionListener associated with the component
     */
    public Object onCall(MuleEventContext context) throws Exception
    {
        WriterMessageAdapter response = new WriterMessageAdapter(new StringWriter(4096));
        String method = context.getMessage().getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY,
            HttpConstants.METHOD_POST);
        if (HttpConstants.METHOD_GET.equalsIgnoreCase(method))
        {
            doGet(context, response);
        }
        else
        {
            doPost(context, response);
        }
        response.getWriter().close();
        return new DefaultMuleMessage(response);
    }

    public void initialise() throws InitialisationException
    {
        if (axis == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("No Axis instance, this component has not been initialized properly."), this);
        }
    }

    public void doGet(MuleEventContext context, WriterMessageAdapter response)
        throws MuleException, IOException
    {
        try
        {
            // We parse a new uri based on the listening host and port with the
            // request parameters appended
            // Using the soap prefix ensures that we use a soap endpoint builder
            EndpointURI endpointUri = context.getEndpointURI();
            //We need to re-parse the URI here because we are only give the listening endpoint, not the actual
            //request endpoint. The request endpoint needs to have the query parameters from the client
            //There is no need to do this for Servlet because it does things differently
            if (!"true".equalsIgnoreCase(context.getEndpointURI().getParams().getProperty("servlet.endpoint")))
            {
                String uri = SoapConstants.SOAP_ENDPOINT_PREFIX + context.getEndpointURI().getScheme()
                                + "://" + context.getEndpointURI().getHost() + ":"
                                + context.getEndpointURI().getPort();
                uri += context.getMessage().getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY, "");
                endpointUri = new MuleEndpointURI(uri);
                endpointUri.initialise();
            }

            AxisEngine engine = getAxis();
            String pathInfo = endpointUri.getPath();
            boolean wsdlRequested = false;
            boolean listRequested = false;

            if (endpointUri.getAddress().endsWith(".jws"))
            {
                throw new AxisFault("Jws not supported by the Mule Axis service");
            }

            String queryString = endpointUri.getQuery();
            if (queryString != null)
            {
                if (queryString.equalsIgnoreCase(SoapConstants.WSDL_PROPERTY))
                {
                    wsdlRequested = true;
                }
                else
                {
                    if (queryString.equalsIgnoreCase(SoapConstants.LIST_PROPERTY))
                    {
                        listRequested = true;
                    }
                }
            }

            boolean hasNoPath = (StringUtils.isEmpty(pathInfo) || pathInfo.equals("/"));
            if (!wsdlRequested && !listRequested && hasNoPath)
            {
                reportAvailableServices(context, response);
            }
            else
            {
                MessageContext msgContext = new MessageContext(engine);
                populateMessageContext(msgContext, context, endpointUri);

                msgContext.setProperty("transport.url", endpointUri.toString());
                if (wsdlRequested)
                {
                    processWsdlRequest(msgContext, response);
                }
                else if (listRequested)
                {
                    processListRequest(response);
                }
                else
                {
                    processMethodRequest(msgContext, context, response, endpointUri);
                }
            }
        }
        catch (AxisFault fault)
        {
            reportTroubleInGet(fault, response);
        }
        catch (Exception e)
        {
            reportTroubleInGet(e, response);
        }
    }

    protected void doPost(MuleEventContext context, WriterMessageAdapter response)
        throws Exception
    {
        String soapAction;
        Message responseMsg;
        AxisEngine engine = getAxis();
        if (engine == null)
        {

            throw new MessagingException(CoreMessages.objectIsNull("Axis Engine"), context.getMessage());
        }
        MessageContext msgContext = new MessageContext(engine);

        String contentType;
        try
        {
            //EndpointURI endpointUri = getEndpoint(context);
            EndpointURI endpointUri = context.getEndpointURI();
            populateMessageContext(msgContext, context, endpointUri);
            if (securityProvider != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("securityProvider:" + securityProvider);
                }
                msgContext.setProperty("securityProvider", securityProvider);
            }

            Object request = context.transformMessage();
            if (request instanceof File)
            {
                request = new FileInputStream((File)request);
            }
            else if (request instanceof byte[])
            {
                request = new ByteArrayInputStream((byte[])request);
            }

            Message requestMsg = new Message(request, false, context.getMessage().getStringProperty(
                HTTPConstants.HEADER_CONTENT_TYPE, null), context.getMessage().getStringProperty(
                HTTPConstants.HEADER_CONTENT_LOCATION, null));

            if (logger.isDebugEnabled())
            {
                logger.debug("Request Message:" + requestMsg);
            }
            msgContext.setRequestMessage(requestMsg);
            msgContext.setProperty("transport.url", endpointUri.toString());

            soapAction = getSoapAction(context);
            if (soapAction != null)
            {
                msgContext.setUseSOAPAction(true);
                msgContext.setSOAPActionURI(soapAction);
            }
            msgContext.setSession(new AxisMuleSession(context.getSession()));

            if (logger.isDebugEnabled())
            {
                logger.debug("Invoking Axis Engine.");
            }
            AxisServiceProxy.setProperties(RequestContext.getEvent().getEndpoint().getProperties());
            engine.invoke(msgContext);
            if (logger.isDebugEnabled())
            {
                logger.debug("Return from Axis Engine.");
            }
            if (RequestContext.getExceptionPayload() instanceof Exception)
            {
                throw (Exception)RequestContext.getExceptionPayload().getException();
            }
            // remove temporary file used for soap message with attachment
            if (request instanceof File)
            {
                ((File)request).delete();
            }
            responseMsg = msgContext.getResponseMessage();
            if (responseMsg == null)
            {
                throw new Exception(Messages.getMessage("noResponse01"));
            }
        }
        catch (AxisFault fault)
        {
            logger.error(fault.toString() + " target service is: " + msgContext.getTargetService()
                            + ". MuleEvent is: " + context.toString(), fault);
            processAxisFault(fault);
            configureResponseFromAxisFault(response, fault);
            responseMsg = msgContext.getResponseMessage();
            if (responseMsg == null)
            {
                responseMsg = new Message(fault);
            }
        }
        catch (Exception e)
        {
            responseMsg = msgContext.getResponseMessage();
            response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "500");
            responseMsg = convertExceptionToAxisFault(e, responseMsg);
        }

        contentType = responseMsg.getContentType(msgContext.getSOAPConstants());

        sendResponse(contentType, response, responseMsg);

        if (logger.isDebugEnabled())
        {
            logger.debug("Response sent.");
        }
    }

    private void reportTroubleInGet(Exception exception, WriterMessageAdapter response)
    {
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
        response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "500");
        response.write("<h2>" + Messages.getMessage("error00") + "</h2>");
        response.write("<p>" + Messages.getMessage("somethingWrong00") + "</p>");
        if (exception instanceof AxisFault)
        {
            AxisFault fault = (AxisFault)exception;
            processAxisFault(fault);
            writeFault(response, fault);
        }
        else
        {
            logger.error(exception.getMessage(), exception);
            response.write("<pre>Exception - " + exception + "<br>");
            response.write("</pre>");
        }
    }

    protected void processAxisFault(AxisFault fault)
    {
        org.w3c.dom.Element runtimeException = fault
            .lookupFaultDetail(Constants.QNAME_FAULTDETAIL_RUNTIMEEXCEPTION);
        if (runtimeException != null)
        {
            logger.info(Messages.getMessage("axisFault00"), fault);
            fault.removeFaultDetail(Constants.QNAME_FAULTDETAIL_RUNTIMEEXCEPTION);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug(Messages.getMessage("axisFault00"), fault);
        }

    }

    private void writeFault(WriterMessageAdapter response, AxisFault axisFault)
    {
        String localizedMessage = XMLUtils.xmlEncodeString(axisFault.getLocalizedMessage());
        response.write("<pre>Fault - " + localizedMessage + "<br>");
        response.write(axisFault.dumpToString());
        response.write("</pre>");
    }

    protected void processMethodRequest(MessageContext msgContext,
                                        MuleEventContext context,
                                        WriterMessageAdapter response,
                                        EndpointURI endpointUri) throws AxisFault
    {
        Properties params = endpointUri.getUserParams();

        String method = (String)params.remove(MuleProperties.MULE_METHOD_PROPERTY);
        if (method == null)
        {
            method = endpointUri.getPath().substring(endpointUri.getPath().lastIndexOf("/") + 1);
        }
        StringBuffer args = new StringBuffer(64);

        Map.Entry entry;
        for (Iterator iterator = params.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            args.append("<").append(entry.getKey()).append(">");
            args.append(entry.getValue());
            args.append("</").append(entry.getKey()).append(">");
        }

        if (method == null)
        {
            response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
            response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "400");
            response.write("<h2>" + Messages.getMessage("error00") + ":  "
                            + Messages.getMessage("invokeGet00") + "</h2>");
            response.write("<p>" + Messages.getMessage("noMethod01") + "</p>");
        }
        else
        {
            invokeEndpointFromGet(msgContext, response, method, args.toString());
        }
    }

    protected void processWsdlRequest(MessageContext msgContext, WriterMessageAdapter response)
        throws AxisFault
    {
        AxisEngine engine = getAxis();
        try
        {
            engine.generateWSDL(msgContext);
            Document doc = (Document)msgContext.getProperty("WSDL");
            if (doc != null)
            {
                response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
                XMLUtils.DocumentToWriter(doc, response.getWriter());
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("processWsdlRequest: failed to create WSDL");
                }
                reportNoWSDL(response, "noWSDL02", null);
            }
        }
        catch (AxisFault axisFault)
        {
            if (axisFault.getFaultCode().equals(Constants.QNAME_NO_SERVICE_FAULT_CODE))
            {
                processAxisFault(axisFault);
                response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "404");
                reportNoWSDL(response, "noWSDL01", axisFault);
            }
            else
            {
                throw axisFault;
            }
        }
    }

    protected void invokeEndpointFromGet(MessageContext msgContext,
                                         WriterMessageAdapter response,
                                         String method,
                                         String args) throws AxisFault
    {
        String body = "<" + method + ">" + args + "</" + method + ">";
        String msgtxt = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body>"
                        + body + "</SOAP-ENV:Body>" + "</SOAP-ENV:Envelope>";
        Message responseMsg = null;
        try
        {
            ByteArrayInputStream istream = new ByteArrayInputStream(msgtxt.getBytes("ISO-8859-1"));
            AxisEngine engine = getAxis();
            Message msg = new Message(istream, false);
            msgContext.setRequestMessage(msg);
            AxisServiceProxy.setProperties(RequestContext.getEvent().getEndpoint().getProperties());
            engine.invoke(msgContext);
            responseMsg = msgContext.getResponseMessage();
            response.setProperty(HTTPConstants.HEADER_CACHE_CONTROL, "no-cache");
            response.setProperty(HTTPConstants.HEADER_PRAGMA, "no-cache");
            if (responseMsg == null)
            {
                throw new Exception(Messages.getMessage("noResponse01"));
            }
        }
        catch (AxisFault fault)
        {
            processAxisFault(fault);
            configureResponseFromAxisFault(response, fault);
            responseMsg = new Message(fault);
        }
        catch (Exception e)
        {
            response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "500");
            responseMsg = convertExceptionToAxisFault(e, responseMsg);
        }
        response.setProperty(HTTPConstants.HEADER_CONTENT_TYPE, "text/xml");
        response.write(responseMsg.getSOAPPartAsString());
    }

    protected void reportServiceInfo(WriterMessageAdapter response, SOAPService service, String serviceName)
    {
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
        response.write("<h1>" + service.getName() + "</h1>");
        response.write("<p>" + Messages.getMessage("axisService00") + "</p>");
        response.write("<i>" + Messages.getMessage("perhaps00") + "</i>");
    }

    protected void processListRequest(WriterMessageAdapter response) throws AxisFault
    {
        AxisEngine engine = getAxis();
        response.setProperty(HTTPConstants.HEADER_CONTENT_TYPE, "text/html");
        if (enableList)
        {
            Document doc = Admin.listConfig(engine);
            if (doc != null)
            {
                XMLUtils.DocumentToWriter(doc, response.getWriter());
            }
            else
            {
                response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "404");
                response.write("<h2>" + Messages.getMessage("error00") + "</h2>");
                response.write("<p>" + Messages.getMessage("noDeploy00") + "</p>");
            }
        }
        else
        {
            response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "403");
            response.write("<h2>" + Messages.getMessage("error00") + "</h2>");
            response.write("<p><i>?list</i> " + Messages.getMessage("disabled00") + "</p>");
        }
    }

    private void reportNoWSDL(WriterMessageAdapter response, String moreDetailCode, AxisFault axisFault)
    {
        response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "404");
        response.setProperty(HTTPConstants.HEADER_CONTENT_TYPE, "text/html");
        response.write("<h2>" + Messages.getMessage("error00") + "</h2>");
        response.write("<p>" + Messages.getMessage("noWSDL00") + "</p>");
        if (moreDetailCode != null)
        {
            response.write("<p>" + Messages.getMessage(moreDetailCode) + "</p>");
        }

    }

    protected void reportAvailableServices(MuleEventContext context, WriterMessageAdapter response)
        throws ConfigurationException, AxisFault
    {
        AxisEngine engine = getAxis();
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
        response.write("<h2>And now... Some Services</h2>");
        String version = MuleManifest.getProductVersion();
        if (version == null)
        {
            version = "Version Not Set";
        }
        response.write("<h5>(Mule - " + version + ")</h5>");
        Iterator i;

        try
        {
            response
                .write("<table width=\"400\"><tr><th>Mule Component Services</th><th>Axis Services</th></tr><tr><td width=\"200\" valign=\"top\">");
            i = engine.getConfig().getDeployedServices();
            listServices(i, response);
            response.write("</td><td width=\"200\" valign=\"top\">");
            i = ((MuleConfigProvider)engine.getConfig()).getAxisDeployedServices();
            listServices(i, response);
            response.write("</td></tr></table>");
        }
        catch (ConfigurationException configException)
        {
            if (configException.getContainedException() instanceof AxisFault)
            {
                throw (AxisFault)configException.getContainedException();
            }
            else
            {
                throw configException;
            }
        }

    }

    private void listServices(Iterator i, WriterMessageAdapter response)
    {
        response.write("<ul>");
        while (i.hasNext())
        {
            ServiceDesc sd = (ServiceDesc)i.next();
            StringBuffer sb = new StringBuffer(512);
            sb.append("<li>");
            String name = sd.getName();
            sb.append(name);
            sb.append(" <a href=\"");
            if (sd.getEndpointURL() != null)
            {
                sb.append(sd.getEndpointURL());
                if (!sd.getEndpointURL().endsWith("/"))
                {
                    sb.append("/");
                }
            }
            sb.append(name);
            sb.append("?wsdl\"><i>(wsdl)</i></a></li>");
            response.write(sb.toString());
            if (sd.getDocumentation() != null)
            {
                response.write("<ul><h6>" + sd.getDocumentation() + "</h6></ul>");
            }
            ArrayList operations = sd.getOperations();
            if (!operations.isEmpty())
            {
                response.write("<ul>");
                OperationDesc desc;
                for (Iterator it = operations.iterator(); it.hasNext();)
                {
                    desc = (OperationDesc)it.next();
                    response.write("<li>" + desc.getName());
                }
                response.write("</ul>");
            }
        }
        response.write("</ul>");
    }

    protected void reportCantGetAxisService(MuleEventContext context, WriterMessageAdapter response)
    {
        response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "404");
        response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, "text/html");
        response.write("<h2>" + Messages.getMessage("error00") + "</h2>");
        response.write("<p>" + Messages.getMessage("noService06") + "</p>");
    }

    private void configureResponseFromAxisFault(WriterMessageAdapter response, AxisFault fault)
    {
        int status = getHttpResponseStatus(fault);
        if (status == 401)
        {
            response.setProperty(HttpConstants.HEADER_WWW_AUTHENTICATE, "Basic realm=\"AXIS\"");
        }
        response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(status));
    }

    private Message convertExceptionToAxisFault(Exception exception, Message responseMsg)
    {
        logger.error(exception.getMessage(), exception);
        if (responseMsg == null)
        {
            AxisFault fault = AxisFault.makeFault(exception);
            processAxisFault(fault);
            responseMsg = new Message(fault);
        }
        return responseMsg;
    }

    protected int getHttpResponseStatus(AxisFault af)
    {
        return af.getFaultCode().getLocalPart().startsWith("Server.Unauth") ? 401 : '\u01F4';
    }

    private void sendResponse(String contentType,
                              WriterMessageAdapter response,
                              Message responseMsg) throws Exception
    {
        if (responseMsg == null)
        {
            response.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, "204");
            if (logger.isDebugEnabled())
            {
                logger.debug("NO AXIS MESSAGE TO RETURN!");
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Returned Content-Type:" + contentType);
            }
                response.setProperty(HttpConstants.HEADER_CONTENT_TYPE, contentType);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                responseMsg.writeTo(baos);
                response.write(baos.toString());
        }
    }

    private void populateMessageContext(MessageContext msgContext,
                                        MuleEventContext context,
                                        EndpointURI endpointUri) throws AxisFault, ConfigurationException
    {
        MuleMessage msg = context.getMessage();

        if (logger.isDebugEnabled())
        {
            logger.debug("MessageContext:" + msgContext);
            logger.debug("HEADER_CONTENT_TYPE:"
                            + msg.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, null));
            logger.debug("HEADER_CONTENT_LOCATION:"
                            + msg.getStringProperty(HttpConstants.HEADER_CONTENT_LOCATION, null));
            logger.debug("Constants.MC_HOME_DIR:" + String.valueOf(getHomeDir()));
            logger.debug("Constants.MC_RELATIVE_PATH:" + endpointUri.getPath());
            logger.debug("HTTPConstants.HEADER_AUTHORIZATION:" + msg.getStringProperty("Authorization", null));
            logger.debug("Constants.MC_REMOTE_ADDR:" + endpointUri.getHost());
        }

        msgContext.setTransportName(transportName);
        msgContext.setProperty("home.dir", getHomeDir());
        msgContext.setProperty("path", endpointUri.getPath());
        msgContext.setProperty(HTTPConstants.MC_HTTP_SERVLET, this);
        msgContext.setProperty(HTTPConstants.MC_HTTP_SERVLETLOCATION, endpointUri.getPath());
        // determine service name
        String serviceName = getServiceName(context, endpointUri);
        // Validate Service path against request path
        SOAPService service = msgContext.getAxisEngine().getConfig().getService(
            new QName(serviceName.substring(1)));

        // if using jms or vm we can skip this
        String scheme = endpointUri.getScheme();
        if (!("vm".equalsIgnoreCase(scheme) 
                        || "jms".equalsIgnoreCase(scheme)
                        || "servlet".equalsIgnoreCase(scheme)))
        {            
            // Component Name is set by Mule so if its null we can skip this check
            if (service.getOption(AxisConnector.SERVICE_PROPERTY_COMPONENT_NAME) != null)
            {
                String servicePath = (String)service.getOption("servicePath");
                if (StringUtils.isEmpty(endpointUri.getPath()))
                {
                    if (!("/" + endpointUri.getAddress()).startsWith(servicePath + serviceName))
                    {
                        throw new AxisFault("Failed to find service: " + "/" + endpointUri.getAddress());
                    }
                }
                //We use ends with rather than starts with because if a servlet binding is used we do not have the full
                //path info when the service is registered.  Track MULE-3931 for more info.
                else if (!endpointUri.getPath().endsWith(servicePath + serviceName))
                {
                    throw new AxisFault("Failed to find service: " + endpointUri.getPath());
                }
            }
        }

        msgContext.setProperty(HTTPConstants.MC_HTTP_SERVLETPATHINFO, serviceName);
        msgContext.setProperty("serviceName", serviceName);

        msgContext.setProperty("Authorization", msg.getStringProperty("Authorization", null));
        msgContext.setProperty("remoteaddr", endpointUri.getHost());
        ServletEndpointContextImpl sec = new ServletEndpointContextImpl();
        msgContext.setProperty("servletEndpointContext", sec);
    }

    private String getSoapAction(MuleEventContext context) throws AxisFault
    {
        String soapAction = context.getMessage().getStringProperty(SoapConstants.SOAP_ACTION_PROPERTY_CAPS, null);
        if (logger.isDebugEnabled())
        {
            logger.debug("Header Soap Action:" + soapAction);
        }

        if (StringUtils.isEmpty(soapAction))
        {
            soapAction = context.getEndpointURI().getAddress();
        }
        return soapAction;
    }

    protected String getServiceName(MuleEventContext context, EndpointURI endpointUri) throws AxisFault
    {
        String serviceName = endpointUri.getPath();
        if (StringUtils.isEmpty(serviceName))
        {
            serviceName = getSoapAction(context);
            serviceName = serviceName.replaceAll("\"", "");
            int i = serviceName.indexOf("/", serviceName.indexOf("//"));
            if (i < -1)
            {
                serviceName = serviceName.substring(i + 2);
            }

        }

        int i = serviceName.lastIndexOf('/');
        if (i > -1)
        {
            serviceName = serviceName.substring(i);
        }
        i = serviceName.lastIndexOf('?');
        if (i > -1)
        {
            serviceName = serviceName.substring(0, i);
        }
        return serviceName;
    }

    public String getTransportName()
    {
        return transportName;
    }

    public void setTransportName(String transportName)
    {
        this.transportName = transportName;
    }

    public boolean isEnableList()
    {
        return enableList;
    }

    public void setEnableList(boolean enableList)
    {
        this.enableList = enableList;
    }

    public String getHomeDir()
    {
        if (homeDir == null)
        {
            //TODO fix homeDir = RegistryContext.getConfiguration().getWorkingDirectory() + DEFAULT_AXIS_HOME;
            homeDir = DEFAULT_AXIS_HOME;
        }
        return homeDir;
    }

    public void setHomeDir(String homeDir)
    {
        this.homeDir = homeDir;
    }

    public AxisServer getAxis()
    {
        return axis;
    }

    public void setAxis(AxisServer axisServer)
    {
        this.axis = axisServer;
    }
}
