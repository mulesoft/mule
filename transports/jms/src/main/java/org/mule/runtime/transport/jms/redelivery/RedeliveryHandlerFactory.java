/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;

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
