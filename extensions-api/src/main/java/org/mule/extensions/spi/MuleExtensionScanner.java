/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.spi;

import org.mule.extensions.api.MuleExtensionsManager;
import org.mule.extensions.introspection.api.MuleExtension;

import java.util.List;

public interface MuleExtensionScanner
{

    List<MuleExtension> scan();

    int scanAndRegister(MuleExtensionsManager muleExtensionsManager);

}
