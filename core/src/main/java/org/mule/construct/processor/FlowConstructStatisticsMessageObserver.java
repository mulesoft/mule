/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.processor;

import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.management.stats.ServiceStatistics;
import org.mule.processor.AbstractMessageObserver;

public class FlowConstructStatisticsMessageObserver extends AbstractMessageObserver
    implements FlowConstructAware
{
    protected FlowConstruct flowConstruct;

    @Override
    public void observe(MuleEvent event)
    {
        if ((flowConstruct.getStatistics().isEnabled())
            && (flowConstruct.getStatistics() instanceof ServiceStatistics))
        {
            // TODO (DDO) consider adding incReceivedEventSync and
            // incReceivedEventASync to org.mule.api.management.stats.Statistics in
            // order to avoid this horrendous cast.
            ServiceStatistics serviceStatistics = (ServiceStatistics) flowConstruct.getStatistics();

            if (event.getEndpoint().getExchangePattern().hasResponse())
            {
                serviceStatistics.incReceivedEventSync();
            }
            else
            {
                serviceStatistics.incReceivedEventASync();
            }
        }
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
