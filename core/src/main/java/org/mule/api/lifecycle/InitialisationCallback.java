/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;


/**
 * <code>InitialisationCallback</code> is used to provide customised initialiation
 * for more complex components. For example, soap services have a custom
 * initialisation that passes the service object to the mule service.
 */
public interface InitialisationCallback
{
    void initialise(Object component) throws InitialisationException;
}
