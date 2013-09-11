/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.quartz.config.AbstractJobConfig;
import org.mule.transport.quartz.config.JobConfig;
import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;

import org.quartz.Job;
import org.quartz.StatefulJob;

/**
 * This configuration simply holds a reference to a user defined job to execute.
 */
public class CustomJobFromMessageConfig extends AbstractJobConfig
{
    private String expression;
    private String evaluator;
    private String customEvaluator;

    public Job getJob(MuleMessage message) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException
    {
        setupEvaluator();

        Object result = getMuleContext().getExpressionManager().evaluate(expression, evaluator, message, true);
        Class<? extends Job> clazz;
        if (result instanceof Job)
        {
            return (Job) result;
        }
        else if (result instanceof JobConfig)
        {
            clazz = ((JobConfig)result).getJobClass();
        }
        else
        {
            throw new IllegalStateException(CoreMessages.propertyIsNotSupportedType(evaluator + ":" + expression,
                    new Class[]{Job.class, JobConfig.class}, result.getClass()).getMessage());
        }

        return ClassUtils.instanciateClass(clazz);
    }

    public JobConfig getJobConfig(MuleMessage message) throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException
    {
        setupEvaluator();

        final MuleContext muleContext = getMuleContext();
        Object result = muleContext.getExpressionManager().evaluate(expression, evaluator, message, true);
        if (result instanceof Job)
        {
            CustomJobConfig customJob = new CustomJobConfig();
            customJob.setJob((Job) result);
            customJob.setMuleContext(muleContext);
            return customJob;
        }
        else if (result instanceof JobConfig)
        {
            if (result instanceof MuleContextAware)
            {
                ((MuleContextAware) result).setMuleContext(muleContext);
            }
            return (JobConfig) result;
        }
        else
        {
            throw new IllegalStateException(CoreMessages.propertyIsNotSupportedType(evaluator + ":" + expression,
                    new Class[]{Job.class, JobConfig.class, Class.class, String.class}, result.getClass()).getMessage());
        }
    }

    protected void setupEvaluator()
    {
        if (evaluator.equals("custom"))
        {
            evaluator = customEvaluator;
        }
    }

    public String getCustomEvaluator()
    {
        return customEvaluator;
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        this.customEvaluator = customEvaluator;
    }

    public String getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = evaluator;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    @Override
    protected Class<? extends StatefulJob> getStatefulJobClass()
    {
        return StatefulCustomJob.class;
    }

    @Override
    protected Class<? extends Job> getStatelessJobClass()
    {
        return CustomJob.class;
    }
}
