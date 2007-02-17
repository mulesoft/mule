/*
 * $Id: Registerable.java 3649 2006-10-24 10:09:08Z holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;


/**
 * <code>Registerable</code> is a lifecycle interface that gets called at the
 * register lifecycle stage of the implementing component.
 */
public interface Registerable
{
    /**
     * Method used to perform any registration of the component
     * to the registry.
     *
     * @throws RegistryException if the registration fails
     */
    //void register() throws RegistryException;

    /**
     * Sets a property for monitoring
     */
    //void setRegistryListenerProperty(String propertyName);

    /**
     * Deregister this component from the registry.
     *
     * @throws RegistryException if the deregistration fails
     */
    //void deregister() throws RegistryException;

    /**
     * Returns a unique ID for the entity which will be used to unequivocally identify it in the registry.
     */
    String getId();
}

