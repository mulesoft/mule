/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.quartz.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.providers.quartz.QuartzConnector;
import org.mule.umo.UMOException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Will dispatch to a Mule endpoint using the Mule client
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientDispatchJob implements Job
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Object payload = jobDataMap.get(QuartzConnector.PROPERTY_PAYLOAD);
        if(payload==null) {
            payload = new NullPayload();
        }
        String dispatchEndpoint = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT);
        if(dispatchEndpoint==null) {
            throw new JobExecutionException(new Message("quartz", 4, QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT).getMessage());
        }
        try {
            MuleClient client = new MuleClient();
            logger.debug("Dispatching payload on: " + dispatchEndpoint);
            client.dispatch(dispatchEndpoint, payload, jobDataMap);
        } catch (UMOException e) {
            throw new JobExecutionException(e);
        }
    }
}

