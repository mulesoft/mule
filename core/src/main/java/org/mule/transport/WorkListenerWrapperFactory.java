/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
