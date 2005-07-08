// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * MessageExchange.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

import java.net.URI;

import javax.jbi.servicedesc.ServiceEndpoint;

import javax.xml.namespace.QName;

/** MessageExchange represents a container for normalized messages which are 
 *  described by an exchange pattern.  The exchange pattern defines the 
 *  names, sequence, and cardinality of messages in an exchange.
 *
 * @author JSR208 Expert Group
 */
public interface MessageExchange
{
    /** JTA transaction context property name. */
    String JTA_TRANSACTION_PROPERTY_NAME = "javax.jbi.transaction.jta";
    
    /** Returns the URI of the pattern for this exchange. 
     *  @return pattern URI for this exchange
     */
    URI getPattern();
    
    /** Returns the unique identifier assigned by the NMS for this exchange. 
     *  @return unique id for this exchange
     */
    String getExchangeId();
    
    /** Returns the processing status of the exchange. 
     * @return status of the exchange
     */
    ExchangeStatus getStatus();
        
    /** Sets the processing status of the exchange.
     *  @param status exchange status
     *  @throws MessagingException failed to set status, possibly due to an 
     *  invalid state transition.
     */
    void setStatus(ExchangeStatus status)
        throws MessagingException;
        
    /** Used to specify the source of a failure status.  Invoking this method 
     *  automatically adjusts the status of the ME to ExchangeStatus.ERROR.
     *  @param error error cause
     */
    void setError(Exception error);
    
    /** Retrieves the Exception describing the exchanges error status.
     *  @return exception associated with this exchange
     */
    Exception getError();
    
    /** Retrieves the fault message for this exchange, if one exists.  A 
     *  fault/message reference is unnecessary, since an exchange can carry 
     *  at most one fault, and it is always the final message in an exchange.
     *  @return fault associated with the exchange, or null if not present
     */
    Fault getFault();
        
    /** Specifies the fault message for this exchange, if one exists. A 
     *  fault/message reference is unnecessary, since an exchange can carry 
     *  at most one fault, and it is always the final message in an exchange.
     *  @param fault fault
     *  @throws MessagingException operation not permitted in the current exchange state
     */
    void setFault(Fault fault)
        throws MessagingException;
    
    /** Creates a normalized message based on the specified message reference.
     *  The pattern governing this exchange must contain a definition for the
     *  reference name supplied.
     *  @return a new normalized message
     *  @throws MessagingException failed to create message
     */
    NormalizedMessage createMessage()
        throws MessagingException;
    
    /** Generic factory method for Fault objects.
     *  @return a new fault
     *  @throws MessagingException failed to create fault
     */
    Fault createFault()
        throws MessagingException;
        
    /** Retrieves a normalized message based on the specified message reference.
     *  @param name message reference
     *  @return message with the specified reference name
     */
    NormalizedMessage getMessage(String name);
        
    /** Sets a normalized message with the specified message reference.
     *  The pattern governing this exchange must contain a definition for the
     *  reference name supplied.
     *  @param msg normalized message
     *  @param name message reference
     *  @throws MessagingException operation not permitted in the current exchange state
     */
    void setMessage(NormalizedMessage msg, String name)
        throws MessagingException;
    
    /** Retrieves the specified property from the exchange.
     *  @param name property name
     *  @return property value
     */
    Object getProperty(String name);
    
    /** Specifies a property for the exchange.
     *  @param name property name
     *  @param obj property value
     */
    void setProperty(String name, Object obj);
    
    /** Specifies the endpoint used by this exchange.
     *  @param endpoint endpoint address
     */
    void setEndpoint(ServiceEndpoint endpoint);
    
    /** Specifies the service used by this exchange.
     *  @param service service address
     */
    void setService(QName service);
    
    /** Specifies the interface name used by this exchange.
     *  @param interfaceName interface name
     */
    void setInterfaceName(QName interfaceName);
    
    /** Specifies the operation used by this exchange.
     *  @param name operation name
     */
    void setOperation(QName name);
    
    /** Retrieves the endpoint used by this exchange.
     *  @return endpoint address for this message exchange
     */
    ServiceEndpoint getEndpoint();
    
    /** Retrieves the interface name used by this exchange.
     *  @return interface used for this message exchange
     */
    QName getInterfaceName();
    
    /** Retrieves the service used by this exchange.
     *  @return service address for this message exchange
     */
    QName getService();
    
    /** Retrieves the operation used by this exchange.
     *  @return operation name for this message exchange
     */
    QName getOperation();
    
    /** Queries the existence of a transaction context.
     *  @return boolean transactional state of the exchange
     */
    boolean isTransacted();

    /** Queries the role that the caller plays in the exchange.
     * @return Role expected of caller.
     */
    Role getRole();
        
    /** Returns the name of all properties for this exchange.
     *  @return a set of all the property names, as Strings.
     */
    java.util.Set getPropertyNames();

    /** Typesafe enum containing the roles a component can play in a service.
     */
    public static final class Role
    {
        /** Service provider. */
        public static final Role PROVIDER = new Role();
        /** Service consumer. */
        public static final Role CONSUMER = new Role();
        /** Prevent direct instantiation. */
        private Role()
        {
        }
    }
}
