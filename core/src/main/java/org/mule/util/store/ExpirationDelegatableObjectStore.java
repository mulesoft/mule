/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

/**
 * Interface to identify those object stores that manage the expiration due to TTL of records independently from mule
 * monitoring
 */
public interface ExpirationDelegatableObjectStore
{

    /**
     * Sets the properties needed for the expiration policy
     * 
     * @param entryTTL the entryTTL of the object store record.
     */
    void setExpirationPolicyProperties(int entryTTL);

    /**
     * Verifies if the actual configuration of the object store allows delegation of expiration
     * 
     * @return whether mule must perform the record expiration in the object store.
     */
    boolean mustPerformMuleExpiration();
}
