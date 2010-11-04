/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

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
        return new PropertyEditorValueToTextTransformer(new sun.beans.editors.BoolEditor(), Boolean.class);
    }

    @Override
    public Object getTestData()
    {
        return "True";
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new PropertyEditorTextToValueTransformer(new sun.beans.editors.BoolEditor(), Boolean.class);
    }

}
