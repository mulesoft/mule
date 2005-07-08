// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * DeliveryChannel.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

import javax.jbi.servicedesc.ServiceEndpoint;

import javax.xml.namespace.QName;

/** Bi-directional communication channel used to interact with the Normalized 
 *  Message Service.
 *
 * @author JSR208 Expert Group
 */
public interface DeliveryChannel
{    
    /** Closes the delivery channel, halting all message traffic.  
     *  @throws MessagingException fatal error while closing channel.
     */
    void close()
        throws MessagingException;
    
    /** Create a message exchange factory. This factory will create exchange
     *  instances with all appropriate properties set to null.
     *  @return a message exchange factory
     */
    MessageExchangeFactory createExchangeFactory();
    
    /** Create a message exchange factory for the given interface name.
     *  @param interfaceName name of the interface for which all exchanges 
     *   created by the returned factory will be set
     *  @return an exchange factory that will create exchanges for the given 
     *   interface; must be non-null
     */
    MessageExchangeFactory createExchangeFactory(QName interfaceName);
    
    /** Create a message exchange factory for the given service name.
     *  @param serviceName name of the service for which all exchanges 
     *   created by the returned factory will be set
     *  @return an exchange factory that will create exchanges for the given 
     *   service; must be non-null
     */
    MessageExchangeFactory createExchangeFactoryForService(QName serviceName);
    
    /** Create a message exchange factory for the given endpoint.
     *  @param endpoint endpoint for which all exchanges created by the
     *   returned factory will be set for
     *  @return an exchange factory that will create exchanges for the 
     *   given endpoint
     */
    MessageExchangeFactory createExchangeFactory(ServiceEndpoint endpoint);    
      
    /** Blocking call used to service a MessageExchange instance which has 
     *  been initiated by another component.  This method supports concurrent
     *  invocation for multi-threaded environments.
     *  @return mesage exchange instance
     *  @throws MessagingException failed to accept
     */
    MessageExchange accept()
        throws MessagingException;
    
    /** Identical to accept(), but returns after specified interval even if 
     *  a message exchange is unavailable.
     *  @param timeout time to wait in milliseconds
     *  @return mesage exchange instance or null if timeout is reached
     *  @throws MessagingException failed to accept
     */
    MessageExchange accept(long timeout)
        throws MessagingException;   
    
    /** Routes a MessageExchange instance through the Normalized Message Service
     *  to the appropriate servicing component. This method supports concurrent
     *  invocation for multi-threaded environments.
     *  @param exchange message exchange to send
     *  @throws MessagingException unable to send exchange
     */
    void send(MessageExchange exchange)
        throws MessagingException;
    
    /** Routes a MessageExchange instance through the Normalized Message Service
     *  to the appropriate servicing component, blocking until the exchange is
     *  returned.  This method supports concurrent invocation for multi-threaded
     *  environments.
     *  @param exchange message exchange to send
     *  @return true if the exchange has been processed and returned by the
     *  servicing component, false otherwise.
     *  @throws MessagingException unable to send exchange
     */
    boolean sendSync(MessageExchange exchange)
        throws MessagingException;
    
    /** Routes a MessageExchange instance through the Normalized Message Service
     *  to the appropriate servicing component, blocking until the specified 
     *  timeout is reached.  This method supports concurrent invocation for 
     *  multi-threaded environments.
     *  @param exchange message exchange to send
     *  @param timeout time to wait in milliseconds
     *  @return true if the exchange has been processed and returned by the
     *  servicing component, false in the case of timeout.
     *  @throws MessagingException unable to send exchange
     */
    boolean sendSync(MessageExchange exchange, long timeout)
        throws MessagingException;    
}
