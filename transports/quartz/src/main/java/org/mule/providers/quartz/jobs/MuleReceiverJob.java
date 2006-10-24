/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.NullPayload;
import org.mule.providers.quartz.QuartzConnector;
import org.mule.providers.quartz.QuartzMessageReceiver;
import org.mule.umo.manager.ObjectNotFoundException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Invokes a Quartz Message receiver with the payload attached to the quartz job
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleReceiverJob implements Job
{

    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
        AbstractMessageReceiver receiver = (AbstractMessageReceiver)map.get(QuartzMessageReceiver.QUARTZ_RECEIVER_PROPERTY);
        Object payload = jobExecutionContext.getJobDetail().getJobDataMap().get(
            QuartzConnector.PROPERTY_PAYLOAD);

        try
        {
            if (payload == null)
            {
                String ref = jobExecutionContext.getJobDetail().getJobDataMap().getString(
                    QuartzConnector.PROPERTY_PAYLOAD_REFERENCE);
                // for backward compatibility check the old payload Class property
                // too
                if (ref == null)
                {
                    ref = jobExecutionContext.getJobDetail().getJobDataMap().getString(
                        QuartzConnector.PROPERTY_PAYLOAD_CLASS_NAME);
                }
                try
                {
                    payload = MuleManager.getInstance().getContainerContext().getComponent(ref);
                }
                catch (ObjectNotFoundException e)
                {
                    logger.warn("There is no payload attached to this quartz job. Sending Null payload");
                    payload = new NullPayload();
                }
            }
            receiver.routeMessage(new MuleMessage(receiver.getConnector().getMessageAdapter(payload)));
        }
        catch (Exception e)
        {
            receiver.handleException(e);
        }
    }
}
