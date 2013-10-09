/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import java.beans.PropertyEditor;

/**
 * <code>PropertyEditorValueToTextTransformer</code> adapts a {@link PropertyEditor}
 * instance allowing it to be used to transform from a specific type to a String.
 */
public class PropertyEditorValueToTextTransformer extends AbstractTransformer implements DiscoverableTransformer
{

    private PropertyEditor propertyEditor;

    /**
     * Give core transformers a slighty higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public PropertyEditorValueToTextTransformer(PropertyEditor propertyEditor, Class<?> clazz)
    {
        registerSourceType(new SimpleDataType<Object>(clazz));
        setReturnDataType(DataTypeFactory.STRING);
        this.propertyEditor = propertyEditor;
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        synchronized (propertyEditor)
        {
            propertyEditor.setValue(src);
            return propertyEditor.getAsText();
        }
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }

}
