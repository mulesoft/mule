/*
 * $Id:
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.filters;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.extras.wssecurity.handlers.MuleWSSInHandler;
import org.mule.extras.wssecurity.headers.WsSecurityHeadersSetter;
import org.mule.impl.MuleMessage;
import org.mule.impl.security.AbstractEndpointSecurityFilter;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.providers.soap.axis.extensions.MuleConfigProvider;
import org.mule.providers.soap.xfire.XFireConnector;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.SecurityProviderNotFoundException;
import org.mule.umo.security.UnsupportedAuthenticationSchemeException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axis.ConfigurationException;
import org.apache.axis.Handler;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.server.AxisServer;
import org.apache.ws.axis.security.WSDoAllReceiver;
import org.apache.ws.axis.security.WSDoAllSender;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.util.dom.DOMInHandler;

public class WsSecurityFilter extends AbstractEndpointSecurityFilter
{
    private String wsDecryptionFile = null;
    private String wsSignatureFile = null;

    public String getWsDecryptionFile()
    {
        return wsDecryptionFile;
    }

    public void setWsDecryptionFile(String wsDecryptionFile)
    {
        this.wsDecryptionFile = wsDecryptionFile;
    }

    public String getWsSignatureFile()
    {
        return wsSignatureFile;
    }

    public void setWsSignatureFile(String wsSignatureFile)
    {
        this.wsSignatureFile = wsSignatureFile;
    }

    /**
     * This method's use is two-fold. First it sets the required security handlers on
     * the service. Secondly, it checks the properties in the message and if there
     * are security properties among them, it sets them on the service.
     */
    protected void authenticateInbound(UMOEvent event) throws UMOException
    {
        Map properties = event.getSession().getComponent().getDescriptor().getProperties();
        if (properties.containsKey("xfire"))
        {
            XFire server = (XFire)properties.get("xfire");
            String pathInfo = event.getEndpoint().getEndpointURI().getPath();

            String serviceName;
            int i = pathInfo.lastIndexOf('/');

            if (i > -1)
            {
                serviceName = pathInfo.substring(i + 1);
            }
            else
            {
                serviceName = pathInfo;
            }

            Service service = server.getServiceRegistry().getService(serviceName);

            // remove security in handlers if present
            Object[] connectorArray = MuleManager.getRegistry().getConnectors().values().toArray();
            XFireConnector connector = null;
            for (i = 0; i < connectorArray.length; i++)
            {
                if (connectorArray[i] instanceof XFireConnector)
                {
                    connector = (XFireConnector)connectorArray[i];
                }
            }

            if (connector != null){
                Object[] outhandlers = service.getOutHandlers().toArray();
                for (i = 0; i < outhandlers.length; i++)
                {
                    connector.getClientOutHandlers().remove(i);
                }
    
                // add security out handlers if not present
                Object[] handlers = service.getInHandlers().toArray();
                boolean isDomInHandlerPresent = false;
                boolean isWss4jInHandlerPresent = false;
                for (i = 0; i < handlers.length; i++)
                {
                    if (handlers[i] instanceof DOMInHandler)
                    {
                        isDomInHandlerPresent = true;
                    }
                    if (handlers[i] instanceof MuleWSSInHandler)
                    {
                        isWss4jInHandlerPresent = true;
                    }
                }
    
                if (!isDomInHandlerPresent)
                {
                    service.addInHandler(new DOMInHandler());
                }
    
                if (!isWss4jInHandlerPresent)
                {
                    service.addInHandler(new MuleWSSInHandler());
                }
    
                // look for security properties in the message
                Properties props = new Properties();
                if (event.getMessage().getProperty("action") != null)
                {
                    props.putAll(getProperties(event));
                }
    
                // put the security properties found in the message, if any, in the
                // service
                if (!props.isEmpty())
                {
                    Object[] keys = props.keySet().toArray();
                    for (i = 0; i < keys.length; i++)
                    {
                        service.setProperty((String)keys[i], props.getProperty((String)keys[i]));
                    }
                }
            }
        }
        else if (properties.containsKey("axisServer"))
        {
            AxisServer server = (AxisServer)event.getSession().getComponent().getDescriptor()
                .getProperties().get("axisServer");
            MuleConfigProvider provider = (MuleConfigProvider)server.getConfig();

            String prefix = event.getEndpoint().getProtocol() + ":";
            String serviceName = event.getEndpoint().getName().substring(prefix.length());
            SOAPService soapService = null;

            // set required security handlers
            try
            {
                soapService = provider.getService(new QName(serviceName));

                Hashtable options = new Hashtable();
                if (event.getMessage().getProperty("action") != null)
                {
                    options.putAll(getProperties(event));
                    soapService.setPropertyParent(options);
                    Handler inHandler = new WSDoAllReceiver();
                    provider.setGlobalRequest(inHandler);
                }
            }
            catch (ConfigurationException e)
            {
                throw new UnsupportedAuthenticationSchemeException(Message.createStaticMessage("A Configurtation Exception occured while configuring WS-Security on Axis "),new MuleMessage(e.getMessage()));
            }
        }
    }

    /**
     * This method secures the outgouing message by setting the required security
     * handlers.
     */
    protected void authenticateOutbound(UMOEvent event)
        throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException
    {
        if (event.getEndpoint().getConnector() instanceof XFireConnector)
        {
            XFireConnector connector = (XFireConnector)event.getEndpoint().getConnector();

            List clientHandlers = new ArrayList();
            List existingOutHandlers = connector.getClientOutHandlers();

            clientHandlers.add("org.codehaus.xfire.util.dom.DOMOutHandler");
            clientHandlers.add("org.codehaus.xfire.security.wss4j.WSS4JOutHandler");

            if (existingOutHandlers == null)
            {
                connector.setClientOutHandlers(clientHandlers);
            }
            else if (!existingOutHandlers
                .contains("org.codehaus.xfire.security.wss4j.WSS4JOutHandler"))
            {
                connector.setClientOutHandlers(clientHandlers);
            }
        }
        else if (event.getEndpoint().getConnector() instanceof AxisConnector)
        {
            AxisServer server = (AxisServer)event.getSession().getComponent().getDescriptor()
                .getProperties().get("axisServer");
            MuleConfigProvider provider = (MuleConfigProvider)server.getConfig();

            String prefix = event.getEndpoint().getProtocol() + ":";
            String serviceName = event.getEndpoint().getName().substring(prefix.length());
            SOAPService soapService = null;

            try
            {
                soapService = provider.getService(new QName(serviceName));

                Hashtable options = new Hashtable();
                if (event.getMessage().getProperty("action") != null)
                {
                    options.putAll(getProperties(event));
                    soapService.setPropertyParent(options);
                }

                Handler outHandler = new WSDoAllSender();
                provider.setGlobalResponse(outHandler);
            }
            catch (ConfigurationException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected void doInitialise() throws InitialisationException
    {
        // Empty... Does not need to do anything for now
    }

    /**
     * This method gets the decryption and the signature property files and returns
     * them as properties to the calling method.
     * 
     * @param event
     * @return
     */
    protected Properties getProperties(UMOEvent event)
    {
        WsSecurityHeadersSetter secHeaders = new WsSecurityHeadersSetter();

        Properties props2 = secHeaders.addSecurityHeaders(event.getMessage());

        Properties props = new Properties();

        if (wsDecryptionFile != null)
        {
            props.put(WSHandlerConstants.DEC_PROP_FILE, wsDecryptionFile);
        }
        if (wsSignatureFile != null)
        {
            props.put(WSHandlerConstants.SIG_PROP_FILE, wsSignatureFile);
        }
        props.putAll(props2);
        return props;
    }
}
