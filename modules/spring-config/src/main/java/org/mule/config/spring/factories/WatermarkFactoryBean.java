/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.transport.polling.watermark.Watermark;
import org.mule.util.store.MuleObjectStoreManager;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

public class WatermarkFactoryBean extends AbstractFactoryBean<Watermark> implements MuleContextAware, AnnotatedObject
{

    public static final String MULE_WATERMARK_PARTITION = "mule.watermark";
    private String variable;
    private String defaultExpression;
    private String updateExpression;
    private ObjectStore objectStore;

    private Map<QName, Object> annotations = new HashMap<QName, Object>();

    private MuleRegistry registry;

    @Override
    public Class<?> getObjectType()
    {
        return Watermark.class;
    }

    @Override
    protected Watermark createInstance() throws Exception
    {
        ObjectStore<?> os = objectStore;
        if (os == null)
        {
            MuleObjectStoreManager mgr = (MuleObjectStoreManager) registry.get(MuleProperties.OBJECT_STORE_MANAGER);
            os = mgr.getUserObjectStore(MULE_WATERMARK_PARTITION, true);
        }

        return new Watermark(os, variable, defaultExpression, updateExpression);
    }

    public void setObjectStore(ObjectStore objectStore)
    {
        this.objectStore = objectStore;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public void setDefaultExpression(String defaultExpression) {
        this.defaultExpression = defaultExpression;
    }

    public void setUpdateExpression(String updateExpression) {
        this.updateExpression = updateExpression;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.registry = context.getRegistry();
    }

    @Override
    public Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    @Override
    public Map<QName, Object> getAnnotations()
    {
        return annotations;
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations)
    {
        this.annotations = annotations;
    }
}
