/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.transformer;

import org.mule.api.transformer.Transformer;
import org.mule.module.scripting.component.Scriptable;
import org.mule.module.scripting.transformer.ScriptTransformer;
import org.mule.transformer.AbstractTransformerTestCase;

import java.util.ArrayList;
import java.util.List;

public class GroovyScriptTransformerTestCase extends AbstractTransformerTestCase
{
    public Transformer getTransformer() throws Exception
    {
        Scriptable script = new Scriptable();
        script.setScriptEngineName("groovy");
        script.setScriptFile("StringToList.groovy");
        script.initialise();
        
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setName("StringToList");
        transformer.setScript(script);
        transformer.initialise();
        return transformer;
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        Scriptable script = new Scriptable();
        script.setScriptFile("ListToString.groovy");
        script.initialise();
        
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setName("ListToString");
        transformer.setScript(script);
        transformer.initialise();
        return transformer;
    }

    public Object getTestData()
    {
        return "this is groovy!";
    }

    public Object getResultData()
    {
        List list = new ArrayList();
        list.add("this");
        list.add("is");
        list.add("groovy!");
        return list;
    }
}
