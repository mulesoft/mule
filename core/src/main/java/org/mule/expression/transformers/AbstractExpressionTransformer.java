/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression.transformers;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This transformer will evaluate one or more expressions on the current message and return the
 * results as an Array. If only one expression is defined it will return the object returned from
 * the expression.
 * <p/>
 * You can use expressions to extract
 * <ul>
 * <li>headers (single, map or list)</li>
 * <li>attachments (single, map or list)</li>
 * <li>payload</li>
 * <li>xpath</li>
 * <li>groovy</li>
 * <li>bean</li>
 * </ul>
 * and more.
 * <p/>
 * This transformer provides a very powerful way to pull different bits of information from the
 * message and pass them to the service.
 */
public abstract class AbstractExpressionTransformer extends AbstractMessageTransformer
{
    protected List<ExpressionArgument> arguments;

    public AbstractExpressionTransformer()
    {
        //No type checking by default
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
        arguments = new ArrayList<ExpressionArgument>(4);
    }

    public void addArgument(ExpressionArgument argument)
    {
        arguments.add(argument);
    }

    public boolean removeArgument(ExpressionArgument argument)
    {
        return arguments.remove(argument);
    }

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *
     */
    @Override
    public void initialise() throws InitialisationException
    {
        if (arguments == null || arguments.size() == 0)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("arguments[]"), this);
        }

        for (Iterator<ExpressionArgument> iterator = arguments.iterator(); iterator.hasNext();)
        {
            ExpressionArgument argument = iterator.next();
            argument.setMuleContext(muleContext);
            argument.setExpressionEvaluationClassLoader(Thread.currentThread().getContextClassLoader());
            try
            {
                argument.validate();
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public List<ExpressionArgument> getArguments()
    {
        return arguments;
    }

    public void setArguments(List<ExpressionArgument> arguments)
    {
        this.arguments = arguments;
    }
}
