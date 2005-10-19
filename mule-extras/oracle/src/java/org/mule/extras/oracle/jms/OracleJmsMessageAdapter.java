package org.mule.extras.oracle.jms;

import oracle.jms.AdtMessage;
import oracle.xdb.XMLType;

import org.mule.providers.jms.JmsMessageAdapter;
import org.mule.umo.MessagingException;

/** 
 * If the message payload is XML, returns the XML as a string.
 * If the message payload is an ADT, simply returns {@code Object.toString()} in 
 * order to avoid a null pointer exception.  
 * Any other message is handled by the standard {@code JmsMessageAdapter}
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageAdapter extends JmsMessageAdapter {

    public OracleJmsMessageAdapter(Object message) throws MessagingException {
    	super(message);
    }
	
    /** If the message payload is XML, returns the XML as an array of bytes.
     * If the message payload is an ADT, simply returns {@code Object.toString().getBytes()} 
     * in order to avoid a null pointer exception.  
     * Any other message is handled by the standard {@code JmsMessageAdapter}
     * 
     * @see JmsMessageAdapter#getPayloadAsBytes */
    public byte[] getPayloadAsBytes() throws Exception {
    	Object jmsMessage = getPayload();
    	if (jmsMessage instanceof AdtMessage) {
    		Object adtMessage = ((AdtMessage) jmsMessage).getAdtPayload();
    		if (adtMessage instanceof XMLType) {
    			return ((XMLType) adtMessage).getBytesValue();
    		}
    		else return adtMessage.toString().getBytes();
    	} 
    	else return super.getPayloadAsBytes();
    }
    	
    /** If the message payload is XML, returns the XML as a string.
     * If the message payload is an ADT, simply returns {@code Object.toString()} in 
     * order to avoid a null pointer exception.  
     * Any other message is handled by the standard {@code JmsMessageAdapter}
     * 
     * @see JmsMessageAdapter#getPayloadAsString */
    public String getPayloadAsString() throws Exception {
    	Object jmsMessage = getPayload();
    	if (jmsMessage instanceof AdtMessage) {
    		Object adtMessage = ((AdtMessage) jmsMessage).getAdtPayload();
    		if (adtMessage instanceof XMLType) {
    			return ((XMLType) adtMessage).getStringVal();
    		}
    		else return adtMessage.toString();
    	} 
    	else return super.getPayloadAsString();
    }
}
