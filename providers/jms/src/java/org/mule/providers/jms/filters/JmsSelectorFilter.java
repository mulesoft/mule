/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jms.filters;

import org.mule.umo.UMOFilter;

/**
 * <code>JmsSelectorFilter</code> is a wrapper for a Jms Selector.
 * This filter should not be called. Instead the JmsConnector sets the selector on
 * the destionation to the expression set on this filer.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsSelectorFilter implements UMOFilter
{
    private String expression = null;

    public boolean accept(Object object)
    {
        throw new UnsupportedOperationException("This filter cannot be called directly");
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
