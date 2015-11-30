/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import java.beans.PropertyEditor;

public class PropertyEditorTextToValueTransformerTestCase extends AbstractTransformerTestCase
{

    @Override
    public Object getResultData()
    {
        return new Boolean(true);
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return new PropertyEditorValueToTextTransformer(getBooleanPropertyEditor(), Boolean.class);
    }

    @Override
    public Object getTestData()
    {
        return "True";
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new PropertyEditorTextToValueTransformer(getBooleanPropertyEditor(), Boolean.class);
    }

    private PropertyEditor getBooleanPropertyEditor() throws Exception
    {
        String[] classNames = {
                "com.sun.beans.editors.BooleanEditor", // java 8
                "sun.beans.editors.BooleanEditor", // java 7
                "sun.beans.editors.BoolEditor"}; // java 6
        ClassNotFoundException exception = null;
        for (String className : classNames)
        {
            try
            {
                return (PropertyEditor) Class.forName(className).newInstance();
            }
            catch (ClassNotFoundException e)
            {
                exception = e;
            }
        }
        throw exception;
    }

}
