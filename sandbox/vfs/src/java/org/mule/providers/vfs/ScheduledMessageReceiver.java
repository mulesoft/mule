/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.mule.providers.vfs;

import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.jobs.NoOpJob;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ian
 * Date: May 16, 2005
 * Time: 11:58:26 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ScheduledMessageReceiver extends AbstractMessageReceiver implements TriggerListener {
  public static final String PROPERTY_TRIGGER = "trigger";
  private static Scheduler scheduler;
  protected Trigger trigger;
  protected JobDetail jobDetail;

  public ScheduledMessageReceiver(UMOConnector connector,
                                  UMOComponent component,
                                  final UMOEndpoint endpoint,
                                  Trigger trigger) throws InitialisationException {
    super(connector, component, endpoint);
    Map props = endpoint.getProperties();

    Trigger overrideTrigger = (Trigger) props.get(PROPERTY_TRIGGER);
    if (overrideTrigger != null) {
      trigger = overrideTrigger;
    }
    else {
      this.trigger = trigger;
    }

    try {
      if (scheduler == null) {
        scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
      }
      jobDetail = new JobDetail(endpoint.getEndpointURI().getPath() + "Job",
            Scheduler.DEFAULT_GROUP,
            NoOpJob.class);
      Trigger clone = (Trigger) trigger.clone();
      clone.setName(getName() + "Trigger");
      scheduler.scheduleJob(jobDetail, clone);
      trigger.addTriggerListener(getName());
      scheduler.addTriggerListener(this);

    }
    catch (SchedulerException e) {
      throw new InitialisationException(e, component);
    }
  }

  public void release() {
    this.dispose();
  }

  protected void doDispose() {
    try {
      scheduler.unscheduleJob(getName() + "Trigger", Scheduler.DEFAULT_GROUP);
    }
    catch (SchedulerException e) {
      logger.error("Could not dispoae", e);
    }
  }

  public void doConnect() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void doDisconnect() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  public Trigger getTrigger() {
    return trigger;
  }

  public UMOConnector getConnector() {
    return connector;
  }

  public UMOComponent getComponent() {
    return component;
  }

  public UMOEndpoint getEndpoint() {
    return endpoint;
  }

  public String getName() {
    return endpoint.getEndpointURI().getPath();
  }

  public  void triggerFired(Trigger trigger, JobExecutionContext jobExecutionContext) {
   
     if (trigger.getName().equals(getName()+"Trigger")) {
       execute(jobExecutionContext.getJobDetail());
     }
  }

  public abstract void execute(JobDetail jobdetail);

  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext jobExecutionContext) {
    return false;
  }

  public void triggerMisfired(Trigger trigger) {
  }

  public void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext, int i) {
  }
}
