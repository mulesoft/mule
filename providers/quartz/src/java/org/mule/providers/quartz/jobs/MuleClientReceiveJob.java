/*
 * $Header$
 * $Revision$
 * $Date$
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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobDataMap;
import org.mule.providers.quartz.QuartzConnector;
import org.mule.config.i18n.Message;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.MuleManager;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Will receive on an endpoint and dispatch the result on another
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientReceiveJob implements Job
{
    /**
     * The logger used for this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());


    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        String dispatchEndpoint = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT);
        if(dispatchEndpoint==null) {
            throw new JobExecutionException(new Message("quartz", 4, QuartzConnector.PROPERTY_JOB_DISPATCH_ENDPOINT).getMessage());
        }

        String receiveEndpoint = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_RECEIVE_ENDPOINT);
        if(receiveEndpoint==null) {
            throw new JobExecutionException(new Message("quartz", 4, QuartzConnector.PROPERTY_JOB_RECEIVE_ENDPOINT).getMessage());
        }
        long timeout = MuleManager.getConfiguration().getSynchronousEventTimeout();
        String timeoutString = jobDataMap.getString(QuartzConnector.PROPERTY_JOB_RECEIVE_TIMEOUT);
        if(timeoutString!=null) {
            timeout = Long.parseLong(timeoutString);
        }
        try {
            MuleClient client = new MuleClient();
            logger.debug("Attempting to receive event on: " + receiveEndpoint);
            UMOMessage result = client.receive(receiveEndpoint, timeout);
            if(result!=null) {
                logger.debug("Received event on: " + receiveEndpoint);
                logger.debug("Dispatching result on: " + dispatchEndpoint);
                result.addProperties(jobDataMap);
                client.dispatch(dispatchEndpoint, result);
            }
        } catch (UMOException e) {
            throw new JobExecutionException(e);
        }
    }
}

