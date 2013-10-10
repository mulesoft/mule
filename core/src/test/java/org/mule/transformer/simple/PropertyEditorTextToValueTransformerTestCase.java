/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
