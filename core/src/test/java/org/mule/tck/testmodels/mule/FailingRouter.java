/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.processor.AbstractFilteringMessageProcessor;

public class FailingRouter extends AbstractFilteringMessageProcessor
{
    @Override
    protected boolean accept(MuleEvent event)
    {
        throw new MuleRuntimeException(MessageFactory.createStaticMessage("Failure"));
    }
}
