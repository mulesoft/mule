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
import java.beans.PropertyEditorManager;

public class PropertyEditorValueToTextTransformerTestCase extends AbstractTransformerTestCase
{
    @Override
    public Object getResultData()
    {
        return "True";
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return new PropertyEditorTextToValueTransformer(createBooleanPropertyEditor(), Boolean.class);
    }

    @Override
    public Object getTestData()
    {
        return new Boolean(true);
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new PropertyEditorValueToTextTransformer(createBooleanPropertyEditor(), Boolean.class);
    }

    private PropertyEditor createBooleanPropertyEditor() throws Exception
    {
        return PropertyEditorManager.findEditor(boolean.class);
    }
}
