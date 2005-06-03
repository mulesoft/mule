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

package org.mule.providers.quartz;

import java.util.Date;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.DefaultMessageAdapter;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ClassHelper;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * TODO: document this class
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class QuartzMessageReceiver extends AbstractMessageReceiver
{

    private static final String PROP_DISPATCHER = "mule.quartz.dispatcher";

    public QuartzMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        super(connector, component, endpoint);
    }

    protected void doDispose()
    {
    }

    public void doStart() throws UMOException
    {
        try {
            QuartzConnector connector = (QuartzConnector) this.connector;
            Scheduler s = connector.getScheduler();

            JobDetail jb = new JobDetail();
            jb.setName(endpoint.getEndpointURI().toString());
            jb.setGroup("mule");
            jb.setJobClass(MuleJob.class);
            JobDataMap map = new JobDataMap();
            map.put(PROP_DISPATCHER, this);
            jb.setJobDataMap(map);

            Trigger trigger = null;
            String cron = getStringProperty(QuartzConnector.PROPERTY_CRON_EXPRESSION);
            String itv = getStringProperty(QuartzConnector.PROPERTY_REPEAT_INTERVAL);
            String cnt = getStringProperty(QuartzConnector.PROPERTY_REPEAT_COUNT);
            String del = getStringProperty(QuartzConnector.PROPERTY_START_DELAY);
            if (cron != null) {
                CronTrigger ctrigger = new CronTrigger();
                ctrigger.setCronExpression(cron);
                trigger = ctrigger;
            } else if (itv != null) {
                SimpleTrigger strigger = new SimpleTrigger();
                strigger.setRepeatInterval(Long.parseLong(itv));
                if (cnt != null) {
                    strigger.setRepeatCount(Integer.parseInt(cnt));
                } else {
                    strigger.setRepeatCount(-1);
                }
                trigger = strigger;
            } else {
                throw new IllegalArgumentException("One of cron or interval property must be set");
            }
            long start = System.currentTimeMillis();
            if (del != null) {
                start += Long.parseLong(del);
            }
            trigger.setStartTime(new Date(start));
            trigger.setName(endpoint.getEndpointURI().toString());
            trigger.setGroup("mule");
            trigger.setJobName(endpoint.getEndpointURI().toString());
            trigger.setJobGroup("mule");

            s.scheduleJob(jb, trigger);
            s.start();
        } catch (Exception e) {
            throw new EndpointException(new Message(Messages.FAILED_TO_START_X, "Quartz receiver"), e);
        }
    }

    private String getStringProperty(String name)
    {
        Object o = endpoint.getProperties().get(name);
        if (o != null) {
            return o.toString();
        } else {
            return null;
        }
    }

    private Object getProperty(String name)
    {
        return endpoint.getProperties().get(name);
    }

    protected void onTrigger()
    {
        try {
            Object payload = getProperty(QuartzConnector.PROPERTY_PAYLOAD);
            if (payload == null) {
                String payloadClassName = getStringProperty(QuartzConnector.PROPERTY_PAYLOAD_CLASS_NAME);
                if (payloadClassName != null) {
                    payload = ClassHelper.instanciateClass(payloadClassName, null);
                }
            }
            routeMessage(new MuleMessage(new DefaultMessageAdapter(payload)));
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void doConnect() throws Exception
    {
        // todo
    }

    public void doDisconnect() throws Exception
    {
        // todo
    }

    public static class MuleJob implements Job
    {

        public void execute(JobExecutionContext context) throws JobExecutionException
        {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            QuartzMessageReceiver receiver = (QuartzMessageReceiver) map.get(PROP_DISPATCHER);
            receiver.onTrigger();
        }

    }
}
