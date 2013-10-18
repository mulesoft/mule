/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkListener;

/**
 * Creates {@link WorkListener} to delegate notifications originated in a
 * work to another {@link WorkListener}
 */
public interface WorkListenerWrapperFactory
{

    WorkListener create(Work work, WorkListener listener);

}
