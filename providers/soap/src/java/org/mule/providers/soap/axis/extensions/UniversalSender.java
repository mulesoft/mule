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
package org.mule.providers.soap.axis.extensions;

import org.apache.axis.AxisFault;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.handlers.BasicHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOMessage;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UniversalSender extends BasicHandler {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void invoke(MessageContext msgContext) throws AxisFault {
        boolean sync = true;
        if (msgContext.isClient() && msgContext.containsProperty("call_object")) {
            Call call = (Call) msgContext.getProperty("call_object");
            if (Boolean.TRUE.equals(call.getProperty("axis.one.way"))) {
                sync = false;
            }
        }

        String uri = msgContext.getStrProp(MessageContext.TRANS_URL);
        try {
            MuleClient client= new MuleClient();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                msgContext.getRequestMessage().writeTo(baos);
            Map props = new HashMap();
            for (Iterator iterator = msgContext.getPropertyNames(); iterator.hasNext();) {
                String name = (String)iterator.next();
                if(!name.equals("call_object") && !name.equals("wsdl.service")) {
                    props.put(name, msgContext.getProperty(name));
                }
            }
            props.put("SOAPAction", uri);
            if(sync) {
                props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
                UMOMessage result = client.send(uri, baos.toByteArray(), props);
                if(result!=null) {
                    byte[] response = result.getPayloadAsBytes();
                    Message responseMessage = new Message(response);
                    msgContext.setResponseMessage(responseMessage);
                } else {
                    logger.warn("No response message was returned from synchronous call to: " + uri);
                }
            } else {
                client.dispatch(uri, baos.toByteArray(), props);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
