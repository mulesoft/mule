/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.registry;

/**
 * The parent interface for all object processors. Object processors can be registered in the Mule regisrty and fired
 * at the correct time.
 *
 * Developers must not implement this interface directly. Instead use either {@link org.mule.api.registry.InjectProcessor} or
 * {@link org.mule.api.registry.PreInitProcessor}. 
 */
public interface ObjectProcessor
{
    Object process(Object object);
}
