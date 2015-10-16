/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import java.io.Serializable;
import java.util.List;

/**
 * Keeps context information about the executing flows and its callers
 * in order to provide augmented troubleshooting information for an application developer.
 * 
 * This will only be enabled it the log is set to debug or if the system property {@code mule.flowCallStacks} is set to {@code true}. If not enabled, the stack will always be empty.
 * 
 * @since 3.8.0
 */
public interface FlowCallStack extends Serializable
{

    /**
     * @return the top-most element of this stack
     */
    FlowStackElement peek();

    /**
     * @return the elements of this stack as a list, ordered from top to bottom.
     */
    List<FlowStackElement> getElements();

    /**
     * @return the paths of the processors that were executed as part of this flow.
     */
    List<String> getExecutedProcessors();

}
