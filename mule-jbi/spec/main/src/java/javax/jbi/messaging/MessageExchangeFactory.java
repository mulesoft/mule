// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * MessageExchangeFactory.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

import java.net.URI;

import javax.xml.namespace.QName;

/** A message exchange factory is used used to create new instances of 
 *  MessageExchange. Service consumers use these factories to create message 
 *  exchanges when initiating a new service request. Message exchange factories
 *  are created using the javax.jbi.component.ComponentContext given to the
 *  component during its initialization (see javax.jbi.component.ComponentLifeCycle ). 
 *  There are a variety of ways to creating such factories, each of which creates
 *  a context that is used to provide some of the default properties of 
 *  MessageExchange instances created by the factory instances. For example, 
 *  a factory can be created for a particular endpoint, ensuring that all 
 *  exchanges created by the factory have that endpoint set as the default 
 *  endpoint property of the exchange. This allows components to retain 
 *  factories as a way of aligning internal processing context with the context 
 *  contained in the factory, ensuring that the exchanges created consistently 
 *  reflect that context.
 * @author JSR208 Expert Group
 */
public interface MessageExchangeFactory
{
    /** Creates a new MessageExchange instance used to initiate a service 
     *  invocation.  JBI defines a set of four basic message exchange types, 
     *  corresponding to the predefined in-* WSDL 2.0 Message Exchange Patterns.
     *  @param serviceName name of the service to be invoked
     *  @param operationName name of the operation to be invoked
     *  @return new message exchange, initialized for invoking the given 
     *   service and operation
     *  @throws MessagingException if the given service or operation are not 
     *   registered with the NMR or the factory was created for a particular 
     *   interface, and the given serviceName does not implement that interface.
     */
    MessageExchange createExchange(QName serviceName, QName operationName)
        throws MessagingException;
    
    /** Creates a new MessageExchange instance used to initiate a service 
     *  invocation. JBI defines a set of eight fundamental message exchange 
     *  types which are created using binding and engine delivery channels. This
     *  base method is provided for extensibility, to satisfy the need for 
     *  vendor-specific message exchange patterns. The registration and 
     *  administration of these patterns is not addressed by JBI.
     *  @param pattern message exchange pattern
     *  @return new message exchange
     *  @throws MessagingException specified pattern is not registered to a 
     *   message exchange type
     */
    MessageExchange createExchange(URI pattern)
        throws MessagingException;
        
    /** Convenience method that creates an In-Only message exchange.
     *  @return new In-Only message exchange
     *  @throws MessagingException failed to create exchange
     */
    InOnly createInOnlyExchange()
        throws MessagingException;
    
    /** Convenience method that creates an In-Optional-Out message exchange.
     *  @return new In-Optional-Out message exchange
     *  @throws MessagingException failed to create exchange
     */
    InOptionalOut createInOptionalOutExchange()
        throws MessagingException;
    
    /** Convenience method that creates an In-Out message exchange.
     *  @return new In-Out message exchange
     *  @throws MessagingException failed to create exchange
     */
    InOut createInOutExchange()
        throws MessagingException;
    
    /** Convenience method that creates an Robust-In-Only message exchange.
     *  @return new Robust-In-Only message exchange
     *  @throws MessagingException failed to create exchange
     */
    RobustInOnly createRobustInOnlyExchange()
        throws MessagingException;    
}
