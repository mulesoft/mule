/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;

/**
 * <code>Registerable</code> is a lifecycle interface that gets called at the
 * register lifecycle stage of the implementing component.
 * 
 * @author 
 * @version $Revision$
 */
public interface Registerable
{
    /**
     * Method used to perform any registration of the component
     * to the registry
     *
     * @throws RegistrationException if the registration fails
     */
    void register() throws RegistrationException;

    /**
     * Sets a property for monitoring
     */
    //void setRegistryListenerProperty(String propertyName);

    /**
     * Degister this component from the registry
     */
    void deregister() throws DeregistrationException;

    /**
     * Returns the registry id.
     * 
     * @return the registry ID
     */
    String getRegistryId();

}

