/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
