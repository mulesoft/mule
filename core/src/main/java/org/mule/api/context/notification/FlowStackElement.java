/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import java.io.Serializable;

/**
 * Keeps context information about the processors that a flow executed.
 * 
 * @since 3.8.0
 */
public interface FlowStackElement extends Serializable
{

    /**
     * @return the path of the currently executing processor in the current flow.
     */
    String currentMessageProcessor();

    /**
     * @return the name of the flow which execution is represented by this element.
     */
    String getFlowName();

}
