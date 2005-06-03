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
package org.mule.config.builders;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * <code>ContainerReference</code> maintains a container reference for the
 * MuleXmlConfigurationBuilder that gets wired once the configuration documents
 * have been loaded
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ContainerReference
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ContainerReference.class);

    private String propertyName;
    private String containerRef;
    private String container;
    private Object object;
    private boolean required;

    public ContainerReference(String propertyName,
                              String containerRef,
                              Object object,
                              boolean required,
                              String container)
    {
        this.propertyName = propertyName;
        this.containerRef = containerRef;
        this.container = container;
        this.object = object;
        this.required = required;
    }

    public void resolveReference(UMOContainerContext ctx) throws ContainerException
    {
        Object comp = null;
        try {
            comp = ctx.getComponent(new ContainerKeyPair(container, containerRef));
        } catch (ObjectNotFoundException e) {
            if (required) {
                throw e;
            } else {
                logger.warn("Component reference not found: " + e.getMessage());
                return;
            }
        }
        try {
            if (object instanceof Map) {
                ((Map) object).put(propertyName, comp);
            } else if (object instanceof List) {
                ((List) object).add(comp);
            } else {
                BeanUtils.setProperty(object, propertyName, comp);
            }
        } catch (Exception e) {
            throw new ContainerException(new Message(Messages.CANT_SET_PROP_X_ON_X_OF_TYPE_X,
                                                     propertyName,
                                                     object.getClass().getName(),
                                                     comp.getClass().getName()));
        }
    }
}
