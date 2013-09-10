/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.NamedObject;
import org.mule.api.lifecycle.Lifecycle;

/**
 * Allows Mule modules and transports to extend core functionality in an
 * application-independent fashion.
 */
public interface MuleCoreExtension extends Lifecycle, NamedObject
{
    // no custom methods
}
