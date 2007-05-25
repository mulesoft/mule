/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

import org.mule.providers.ejb.i18n.EjbMessages;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;

import javax.ejb.EJBHome;
import javax.naming.NamingException;

/**
 * <code>EjbContainerContext</code> is a container implementaiton that allows EJB
 * Session beans to be referenced as Mule managed UMOs.
 *
 * This no longer needed in 2.0 since Spring provides this functionality directly.
 */
public class EjbContainerContext extends RmiContainerContext
{
    public EjbContainerContext()
    {
        super("ejb");
    }

    protected EjbContainerContext(String name)
    {
        super(name);
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        Object homeObject = null;
        if (key == null)
        {
            throw new ObjectNotFoundException("null");
        }
        try
        {
            homeObject = context.lookup(key.toString());
        }
        catch (NamingException e)
        {
            throw new ObjectNotFoundException(key.toString(), e);
        }

        if (homeObject == null)
        {
            throw new ObjectNotFoundException(key.toString());
        }
        else if (homeObject instanceof EJBHome)
        {

            Method method = ClassUtils.getMethod(homeObject.getClass(), "create", null);
            if (method == null)
            {
                throw new ObjectNotFoundException(key.toString(), new IllegalArgumentException(
                    EjbMessages.ejbObjectMissingCreate(key).toString()));
            }
            try
            {
                return method.invoke(homeObject, ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new ObjectNotFoundException(key.toString(), e);
            }
        }
        else
        {
            throw new ObjectNotFoundException(key.toString(), new IllegalArgumentException(
                EjbMessages.ejbKeyRefNotValid(key).toString()));
        }
    }
}
