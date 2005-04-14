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
package org.mule.interceptors;

import org.apache.commons.jxpath.JXPathContext;
import org.mule.impl.MuleMessage;
import org.mule.umo.Invocation;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>JXPathNormalizerInterceptor</code> can be used as a simple pre/post
 * message transformer for a given component.
 * <p/>
 * Users can set JXPath expressions to execute before and after the component
 * reeives the event.
 * The <i>beforeExpressions</i> can be a single expression or a comma separated list
 * of expressions, each of which result in an object that will be used as an argument 
 * to the method called on the component.
 *
 * The <i>afterExpression</i> is a single expression that will be used to set a
 * value on the orginal payload.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JXPathNormalizerInterceptor extends MessageNormalizerInterceptor
{
    private List beforeExpressionsList;
    private String beforeExpressions;
    private String afterExpression;

    /**
     * This method is invoked before the event is processed
     *
     * @param invocation the message invocation being processed
     */
    public UMOMessage before(Invocation invocation) throws UMOException
    {
        if (beforeExpressions != null && beforeExpressionsList.size() > 0)
        {
            JXPathContext ctx = JXPathContext.newContext(getOriginalPayload());
            Object[] result = new Object[beforeExpressionsList.size()];
            for (int i = 0; i < result.length; i++)
            {
                result[i] = ctx.getValue((String)beforeExpressionsList.get(i));
            }
            if(result.length==1) {
                return new MuleMessage(result[0], invocation.getMessage().getProperties());
            } else {
                return new MuleMessage(result, invocation.getMessage().getProperties());
            }
        }
        return null;
    }

    /**
     * This method is invoked after the event has been processed
     *
     * @param invocation the message invocation being processed
     */
    public UMOMessage after(Invocation invocation) throws UMOException
    {
        if (afterExpression != null)
        {
            JXPathContext ctx = JXPathContext.newContext(getOriginalPayload());
            ctx.setValue(afterExpression, invocation.getMessage().getPayload());
            return new MuleMessage(getOriginalPayload(), invocation.getMessage().getProperties());
        }
        return null;
    }


    public String getBeforeExpressions()
    {
        return beforeExpressions;
    }

    public void setBeforeExpressions(String beforeExpressions)
    {
        this.beforeExpressions = beforeExpressions;
        String[] exp = Utility.split(beforeExpressions, ",");
        this.beforeExpressionsList = new ArrayList(exp.length);
        for (int i = 0; i < exp.length; i++)
        {
            this.beforeExpressionsList.add(exp[i]);

        }
    }

    public String getAfterExpression()
    {
        return afterExpression;
    }

    public void setAfterExpression(String afterExpression)
    {
        this.afterExpression = afterExpression;
    }
}
