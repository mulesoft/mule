/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.registry;

/**
 * Parent interface for any pluggable service in Mule (transport, model, etc.)
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface ServiceDescriptor
{

    String getService();

}


