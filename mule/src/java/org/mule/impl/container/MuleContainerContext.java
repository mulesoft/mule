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
 *
 */

package org.mule.impl.container;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ClassUtils;

import java.io.Reader;

/**
 * <code>MuleContainerContext</code> is a default resolver that doesn't
 * support external reference resolution. It's function is to provide a complete
 * implementation when a componenet resolver is not defined. The default
 * behaviour is to build a component key as a fully qualified class
 * name
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleContainerContext extends AbstractContainerContext
{
    public static final String MULE_CONTAINER_NAME = "mule";

    public MuleContainerContext()
    {
        super(MULE_CONTAINER_NAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.UMOContainerContext#getComponent(java.lang.Object)
     */
    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        if (key == null) {
            throw new ObjectNotFoundException("Component not found for null key");
        }
        try {
            Class clazz;
            if (key instanceof Class) {
                clazz = (Class) key;
            } else {
                clazz = ClassUtils.loadClass(key.toString(), getClass());
            }
            return clazz.newInstance();
        } catch (Exception e) {
            throw new ObjectNotFoundException(key.toString(), e);
        }
    }

    public void configure(Reader configuration)
    {
        throw new UnsupportedOperationException("configure(Reader)");
    }

    public void initialise() throws InitialisationException
    {
        // nothing to do
    }

}
