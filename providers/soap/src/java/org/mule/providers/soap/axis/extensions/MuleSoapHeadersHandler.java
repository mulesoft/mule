/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis.extensions;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import org.mule.config.MuleProperties;
import org.mule.providers.soap.axis.MuleSoapHeaders;
import org.mule.umo.UMOEvent;

import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;

/**
 * <code>MuleSoapHeadersHandler</code> is an Axis handler that can read and
 * write Mule header properties to a Soap message
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MuleSoapHeadersHandler extends BasicHandler
{
    public void invoke(MessageContext msgContext) throws AxisFault {
        boolean setMustUnderstand =
            msgContext.isPropertyTrue("MULE_HEADER_MUST_UNDERSTAND");

        try {
            if (msgContext.isClient()) {
                if (!msgContext.getPastPivot()) {
                    processClientRequest(msgContext, setMustUnderstand);
                } else {
                   processClientResponse(msgContext);
                }
            } else {
                if (!msgContext.getPastPivot()) {
                    processServerRequest(msgContext);
                } else {
                    processServerResponse(msgContext, setMustUnderstand);
                }
            }
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * Method processClientRequest
     *
     * @param msgContext
     */
    protected synchronized void processClientRequest(MessageContext msgContext,
                                        boolean setMustUnderstand)
        throws Exception {

        SOAPMessageContext soapMsgContext = (SOAPMessageContext) msgContext;
         SOAPMessage msg = soapMsgContext.getMessage();
         if ( msg == null )
         {
            return ;
         }
         UMOEvent event = (UMOEvent) msgContext.getProperty( MuleProperties.MULE_EVENT_PROPERTY );

         if ( event == null )
         {
            return;
         } else {
             synchronized(msgContext) {
            MuleSoapHeaders headers = new MuleSoapHeaders(event);
            headers.addHeaders(msgContext.getMessage().getSOAPPart().getEnvelope());
             }
         }
    }

    /**
     * Method processClientResponse
     *
     * @param msgContext
     */
    protected void processClientResponse(MessageContext msgContext)
        throws Exception {

        SOAPMessageContext soapMsgContext = (SOAPMessageContext) msgContext;

         SOAPMessage msg = soapMsgContext.getMessage();
         if ( msg == null )
         {
            return;
         }
         SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
         MuleSoapHeaders headers = new MuleSoapHeaders(env.getHeader());
         headers.setAsClientProperties(msgContext);
        //msgContext.setProperty(MuleSoapHeaders.ENV_REQUEST_HEADERS, headers );
    }

    /**
     * Method processServerRequest
     *
     * @param msgContext
     * @throws Exception
     */
    protected void processServerRequest(MessageContext msgContext)
        throws Exception {
        SOAPMessageContext soapMsgContext = (SOAPMessageContext) msgContext;

         SOAPMessage msg = soapMsgContext.getMessage();
         if ( msg == null )
         {
            return;
         }
         MuleSoapHeaders headers = new MuleSoapHeaders(msg.getSOAPPart().getEnvelope().getHeader());
         msgContext.setProperty(MuleSoapHeaders.ENV_REQUEST_HEADERS, headers );
    }


    /**
     * Method processServerResponse
     *
     * @param msgContext
     */
    protected void processServerResponse(MessageContext msgContext,
                                         boolean setMustUnderstand)
        throws Exception {

         SOAPMessageContext soapMsgContext = (SOAPMessageContext) msgContext;
         SOAPMessage msg = soapMsgContext.getMessage();
         if ( msg == null )
         {
            return ;
         }
         MuleSoapHeaders headers = (MuleSoapHeaders)msgContext.getProperty( MuleSoapHeaders.ENV_REQUEST_HEADERS );

         if ( headers == null )
         {
            return;
         } else {
            headers.addHeaders(msgContext.getMessage().getSOAPPart().getEnvelope());
         }
    }

}
