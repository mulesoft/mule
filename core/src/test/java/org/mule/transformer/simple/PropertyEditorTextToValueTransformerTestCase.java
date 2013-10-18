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
    
    private PropertyEditor getBooleanPropertyEditor() throws Exception {
        PropertyEditor editor = null;
        
        try
        {
            // try first with java 7 package name
            editor = (PropertyEditor) Class.forName("sun.beans.editors.BooleanEditor").newInstance();
        }
        catch (ClassNotFoundException e)
        {
            // if that doesn't exist, try with java 6 and lower package name
            editor = (PropertyEditor) Class.forName("sun.beans.editors.BoolEditor").newInstance();   
        }
        return editor;
    }

}
