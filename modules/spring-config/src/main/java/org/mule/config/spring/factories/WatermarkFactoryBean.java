/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;
import org.mule.transport.polling.watermark.UpdateExpressionWatermark;
import org.mule.transport.polling.watermark.Watermark;
import org.mule.transport.polling.watermark.selector.SelectorWatermark;
import org.mule.transport.polling.watermark.selector.WatermarkSelectorBroker;
import org.mule.util.store.MuleObjectStoreManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class WatermarkFactoryBean extends AbstractFactoryBean<Watermark>
    implements MuleContextAware, AnnotatedObject
{

    public static final String MULE_WATERMARK_PARTITION = "mule.watermark";
    private static final String DEFAULT_SELECTOR_EXPRESSION = "#[payload]";

    private String variable;
    private String defaultExpression;
    private String updateExpression;
    private WatermarkSelectorBroker selector;
    private String selectorExpression;
    private ObjectStore<Serializable> objectStore;

    private Map<QName, Object> annotations = new HashMap<QName, Object>();
    private MuleContext muleContext;

    @Override
    public Class<?> getObjectType()
    {
        return Watermark.class;
    }

    @Override
    protected Watermark createInstance() throws Exception
    {
        if (this.selector != null)
        {
            if (!StringUtils.isEmpty(this.updateExpression))
            {
                throw new IllegalArgumentException(
                    "You specified a watermark with both an update expression and a selector and/or a selector.\n"
                                    + "Those cannot co-exist. You have to either specify an updateExpression or selector options");
            }
            String selectorExpression = StringUtils.isEmpty(this.selectorExpression)
                                                                                    ? DEFAULT_SELECTOR_EXPRESSION
                                                                                    : this.selectorExpression;

            return new SelectorWatermark(this.acquireObjectStore(), this.variable, this.defaultExpression,
                this.selector, selectorExpression);
        }
        else
        {
            return new UpdateExpressionWatermark(this.acquireObjectStore(), this.variable,
                this.defaultExpression, updateExpression);
        }

    }

    private ObjectStore<Serializable> acquireObjectStore()
    {
        ObjectStore<Serializable> os = this.objectStore;
        if (os == null)
        {
            MuleObjectStoreManager mgr = (MuleObjectStoreManager) this.muleContext.getObjectStoreManager();
            os = mgr.getUserObjectStore(MULE_WATERMARK_PARTITION, true);
        }
        return os;
    }

    public void setObjectStore(ObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    public void setVariable(String variable)
    {
        this.variable = variable;
    }

    public void setDefaultExpression(String defaultExpression)
    {
        this.defaultExpression = defaultExpression;
    }

    public void setUpdateExpression(String updateExpression)
    {
        this.updateExpression = updateExpression;
    }

    public void setSelector(WatermarkSelectorBroker selector)
    {
        this.selector = selector;
    }

    public void setSelectorExpression(String selectorExpression)
    {
        this.selectorExpression = selectorExpression;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
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
