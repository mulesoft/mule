/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.support;

import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.AbstractModel;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

/**
 * TODO
 */
public class InheritedModel extends AbstractModel
{
    public String getType()
    {
        return "inherited";
    }


    /*
    * (non-Javadoc)
    *
    * @see org.mule.umo.UMOModel#getName()
    */
    //@Override
    public String getName()
    {
        return super.getName() + "#" + hashCode();
    }

    public String getParentName()
    {
        return super.getName();
    }

    private class InheritedComponent extends AbstractComponent
    {
        public InheritedComponent()
        {
            super();
        }

        protected UMOMessage doSend(UMOEvent event) throws UMOException
        {
            throw new UnsupportedOperationException("doSend()");
        }

        protected void doDispatch(UMOEvent event) throws UMOException
        {
            throw new UnsupportedOperationException("doDispatch()");
        }
    }

}
