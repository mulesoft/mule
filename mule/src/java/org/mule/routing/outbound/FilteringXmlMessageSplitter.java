/*
* $Header: 
* $Revision: 
* $Date: 
* ------------------------------------------------------------------------------------------------------
* 
 * Copyright (c) Lajos Moczar. All rights reserved.
 * http://www.galatea.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.outbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.DocumentHelper;

import org.mule.impl.MuleMessage;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.routing.outbound.AbstractMessageSplitter;

/**
 * <code>FilteringXmlMessageSplitter</code> will split a DOM4J document
 * into nodes based on the "splitExpression" property.
 *
 * Optionally, you can specify a "namespaces" property map that contain
 * prefix/ns mappings
 *
 * Note that each part returned is actually returned as a new Document
 * 
 * @author <a href="mailto:lajos@galatea.com">Lajos Moczar</a>
 * @version 
 */
public class FilteringXmlMessageSplitter extends AbstractMessageSplitter {
    private Map properties;
    private List nodes;
    private org.dom4j.Document dom4jDoc;
    private String splitExpression = "";
    private Map namespaces = null;

    public void setSplitExpression(String splitExpression) {
    	this.splitExpression = splitExpression;
    }
    
    public void setNamespaces(Map namespaces) {
    	this.namespaces = namespaces;
    }
    
    public String getSplitExpression() {
    	return splitExpression;
    }
    
    /**
     * Template method can be used to split the message up before the
     * getMessagePart method is called .
     *
     * @param message the message being routed
     */
    protected void initialise(UMOMessage message)
    {
    	if (logger.isDebugEnabled()) {
            logger.debug("splitExpression is " + splitExpression);
    	}
    	
        if (message.getPayload() instanceof org.dom4j.Document) {
        	 dom4jDoc = (org.dom4j.Document) message.getPayload();
        	 XPath xpath = dom4jDoc.createXPath(splitExpression);
        	 if (namespaces != null) xpath.setNamespaceURIs( namespaces ); 
        	 nodes = xpath.selectNodes( dom4jDoc );
        } else {
        	logger.error("Message is not a dom4j.Document! It is: " + message.getPayload().getClass().toString());
        }
        
        properties = message.getProperties();
    }

    /**
     * Retrieves a specific message part for the given endpoint. the message
     * will then be routed via the parovider.
     *
     * @param message  the current message being processed
     * @param endpoint the endpoint that will be used to route the resulting
     *                 message part
     * @return the message part to dispatch
     */
    protected UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint)
    {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = (Node)nodes.get(i);
            
            try {
	            UMOMessage result = new MuleMessage(DocumentHelper.parseText(node.asXML()), new HashMap(properties));
	
	            if (endpoint.getFilter() == null || endpoint.getFilter().accept(result)) {
	                if (logger.isDebugEnabled()) {
	                    logger.debug("Endpoint filter matched. Routing message over: "
	                                 + endpoint.getEndpointURI().toString());
	                }
	                nodes.remove(i);
	                return result;
	            }
            } catch (Exception e) {
            	logger.error("Unable to create message for node as position " + i, e);
            	return null;
            }
        }
        return null;
    }
}
