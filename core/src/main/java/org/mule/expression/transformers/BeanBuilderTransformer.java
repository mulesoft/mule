/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.object.PrototypeObjectFactory;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This transformer uses the returnClass to create the return object and then will populate the bean
 * with arguments defined as expressions
 */
public class BeanBuilderTransformer extends AbstractExpressionTransformer
{
    private ObjectFactory beanFactory;
    private Class<?> beanClass;

    public Class<?> getBeanClass()
    {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass)
    {
        this.beanClass = beanClass;
    }

    public ObjectFactory getBeanFactory()
    {
        return beanFactory;
    }

    public void setBeanFactory(ObjectFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();

        if(getBeanFactory()==null && getBeanClass()==null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("beanFactory"), this);
        }
        else if(getBeanClass()!=null)
        {
            setBeanFactory(new PrototypeObjectFactory(getBeanClass()));
        }
        setReturnDataType(DataTypeFactory.create(getBeanFactory().getObjectClass()));
        //We need to set the MuleContext if we create the factory here
        if(getBeanFactory() instanceof MuleContextAware)
        {
            ((MuleContextAware)getBeanFactory()).setMuleContext(muleContext);
        }
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object bean;
        try
        {
            bean = getBeanFactory().getInstance(muleContext);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        Map<String, Object> args = new HashMap<String, Object>(arguments.size());

        for (Iterator<ExpressionArgument> iterator = arguments.iterator(); iterator.hasNext();)
        {
            ExpressionArgument argument = iterator.next();
            Object value = null;
            try
            {
                value = argument.evaluate(message);
            }
            catch (RequiredValueException e)
            {
                logger.warn(e.getMessage());
            }
            catch (ExpressionRuntimeException e)
            {
                throw new TransformerException(this, e);
            }

            if (!argument.isOptional() && value == null)
            {
                throw new TransformerException(CoreMessages.expressionEvaluatorReturnedNull(
                        argument.getExpressionConfig().getEvaluator(), argument.getExpressionConfig().getExpression()), this);

            }
            args.put(argument.getName(), value);
        }

        try
        {
            BeanUtils.populate(bean, args);
        }
        catch (IllegalAccessException e)
        {
            throw new TransformerException(this, e);
        }
        catch (InvocationTargetException e)
        {
            throw new TransformerException(this, e.getTargetException());
        }

        return bean;
    }
}
