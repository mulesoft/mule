/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.spi;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.module.ibeans.spi.support.DataTypeConverter;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import java.lang.reflect.Method;

import org.ibeans.annotation.Return;
import org.ibeans.api.AbstractCallInterceptor;
import org.ibeans.api.InvocationContext;
import org.ibeans.api.Response;
import org.ibeans.spi.ExpressionParser;

/**
 * An interceptor used to process the {@link org.ibeans.annotation.Return} annotation.  This interceptor also
 * performs automatic transformation
 */
public class MuleResponseTransformInterceptor extends AbstractCallInterceptor
{
    private MuleContext muleContext;
    private ExpressionParser parser;

    public MuleResponseTransformInterceptor(MuleContext muleContext, ExpressionParser parser)
    {
        this.muleContext = muleContext;
        this.parser = parser;
    }

    @Override
    public void afterCall(InvocationContext invocationContext) throws Throwable
    {
        MuleResponseMessage result = (MuleResponseMessage)invocationContext.getResponse();
        if (result == null || result.getPayload() == null || invocationContext.getMethod().getReturnType().equals(Void.TYPE))
        {
            return;
        }

        Object finalResult;
        Method method = invocationContext.getMethod();
        //Special handling for Mock ibean methods, need to find a better way of doing this
        if(method.getName().startsWith("ibean"))
        {
            invocationContext.setResult(result.getPayload());
            return;
        }
        DataType requiredType = DataTypeConverter.convertIBeansToMule(invocationContext.getReturnType()); // DataTypeFactory.createFromReturnType(method);

        if (method.getAnnotation(Return.class) != null)
        {
            String returnExpression = method.getAnnotation(Return.class).value();

            finalResult = handleReturnAnnotation(returnExpression, result, invocationContext);

            DataType finalType = DataTypeFactory.createFromObject(finalResult);
            if (!requiredType.isCompatibleWith(finalType))
            {
                Transformer transformer = muleContext.getRegistry().lookupTransformer( finalType, requiredType);
                finalResult = transformer.transform(finalResult);
            }
        }
        else
        {

            if (requiredType.getType().equals(MuleMessage.class))
            {
                finalResult = result.getMessage();
            }
            else if (requiredType.getType().equals(Response.class))
            {
                finalResult = result;
            }
            else
            {
                DataType sourceType = DataTypeFactory.createFromObject(result.getMessage());
                if(requiredType.isCompatibleWith(sourceType))
                {
                    finalResult = result.getPayload();
                }
                else
                {
                    Transformer transformer = muleContext.getRegistry().lookupTransformer(sourceType, requiredType);
                    finalResult = transformer.transform(result.getPayload());
                }
            }


        }
        if(finalResult instanceof NullPayload)
        {
            finalResult = null;
        }
        invocationContext.setResult(finalResult);
    }

    protected Object handleReturnAnnotation(String expr, Response message, InvocationContext ctx)
    {
        if (parser.hasUriTokens(expr))
        {
            expr = parser.parseUriTokens(ctx.getIBeanConfig().getUriParams(), expr);
            expr = parser.parseUriTokens(ctx.getIBeanConfig().getHeaderParams(), expr);
            expr = parser.parseUriTokens(ctx.getIBeanConfig().getPropertyParams(), expr);
        }

        if(ctx.getMethod().getReturnType().equals(Boolean.class))
        {
            return new ExpressionFilter(expr).accept(((MuleResponseMessage)message).getMessage());
        }
        return parser.evaluate(expr, message);
    }
}
