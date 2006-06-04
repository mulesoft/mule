/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.container;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ClassUtils;

import javax.ejb.EJBHome;
import javax.naming.NamingException;

import java.lang.reflect.Method;

/**
 * <code>EjbContainerContext</code> is a container implementaiton that
 * allows EJB Session beans to be referenced as Mule managed UMOs
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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

    public Object getComponent(Object key) throws ObjectNotFoundException {
        Object homeObject = null;
        if(key==null) {
            throw new ObjectNotFoundException("null");
        }
        try {
            homeObject = context.lookup(key.toString());
        } catch (NamingException e) {
            throw new ObjectNotFoundException(key.toString(), e);
        }

        if(homeObject==null) {
            throw new ObjectNotFoundException(key.toString());
        } else if(homeObject instanceof EJBHome) {

            Method method = ClassUtils.getMethod("create", null, homeObject.getClass());
            if(method==null) {
                throw new ObjectNotFoundException(key.toString(),
                        new IllegalArgumentException(new Message(Messages.EJB_OBJECT_X_MISSING_CREATE, key).toString()));
            }
            try {
                return method.invoke(homeObject, ClassUtils.NO_ARGS);
            } catch (Exception e) {
                throw new ObjectNotFoundException(key.toString(), e);
            }
        } else {
            throw new ObjectNotFoundException(key.toString(),
                    new IllegalArgumentException(new Message(Messages.EJB_KEY_REF_X_NOT_VALID, key).toString()));
        }
    }
}