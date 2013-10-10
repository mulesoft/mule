/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.construct;
/**
* Implemented by objects that require the {@link FlowConstruct} to be injected.
*/
public interface FlowConstructAware
{
    void setFlowConstruct(FlowConstruct flowConstruct);
}
