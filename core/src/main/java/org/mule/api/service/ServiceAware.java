/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.service;

/**
 * <code>ServiceAware</code> is an injector interface that will supply a
 * Service to the object. This interface should be implemented by services
 * managed by Mule that want to receive their Service instance. The
 * Service will be set before any initialisation method is called. i.e. if the
 * service implements org.mule.api.lifecycle.Initialisable, the descriptor will be
 * set before initialise() method is called.
 * 
 * @see org.mule.api.lifecycle.Initialisable
 * @see Service
 */
public interface ServiceAware
{

    void setService(Service service);

}
