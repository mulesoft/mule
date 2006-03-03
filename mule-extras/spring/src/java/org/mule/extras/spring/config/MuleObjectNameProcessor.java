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
package org.mule.extras.spring.config;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * <code>MuleObjectNameProcessor</code> is used to set spring ids to Mule
 * object names so the the bean id and name property on the object don't both
 * have to be set.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleObjectNameProcessor implements BeanPostProcessor
{
    private boolean overwrite = false;

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {
        if (!MuleObjectHelper.class.getName().equals(s)) {
            if (o instanceof UMOConnector) {
                if (((UMOConnector) o).getName() == null || overwrite) {
                    ((UMOConnector) o).setName(s);
                }
            } else if (o instanceof UMOTransformer) {
                ((UMOTransformer) o).setName(s);
            } else if (o instanceof UMOEndpoint) {
                // spring uses the class name of the object as the name if no
                // other id is set
                // this is no good for endpoints
                if ((((UMOEndpoint) o).getName() == null || overwrite) && !MuleEndpoint.class.getName().equals(s)) {
                    ((UMOEndpoint) o).setName(s);
                }
            } else if (o instanceof UMODescriptor) {
                if (((UMODescriptor) o).getName() == null || overwrite) {
                    ((UMODescriptor) o).setName(s);
                }
            } else if (o instanceof UMOModel) {
                if (((UMOModel) o).getName() == null || overwrite) {
                    ((UMOModel) o).setName(s);
                }
            } else if (o instanceof UMOAgent) {
                    ((UMOAgent) o).setName(s);
            }
        }
        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        return o;
    }

    public boolean isOverwrite()
    {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

}
