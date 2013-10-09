/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.NamedObject;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;

/**
 * Allows Mule modules and transports to extend core functionality in an
 * application-independent fashion.
 */
public interface MuleCoreExtension extends Initialisable, Disposable, NamedObject
{
    // no custom methods
}
