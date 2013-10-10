/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;

/**
 * Implementors of this interface are responsible to create {@link RedeliveryHandler}
 * instances upon request.
 * <p>
 * Note that due to the stateful nature of a {@link RedeliveryHandler} this factory
 * should always return new {@link RedeliveryHandler} instances.
 */
public interface RedeliveryHandlerFactory
{
    
    /**
     * @return {@link RedeliveryHandler} new redelivery handler instance.
     */
    RedeliveryHandler create();
    
}
