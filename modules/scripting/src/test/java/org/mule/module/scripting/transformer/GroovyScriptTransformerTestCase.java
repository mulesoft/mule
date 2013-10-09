/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.transformer;

import org.mule.api.transformer.Transformer;
import org.mule.module.scripting.component.Scriptable;
import org.mule.transformer.AbstractTransformerTestCase;

import java.util.ArrayList;
import java.util.List;

public class GroovyScriptTransformerTestCase extends AbstractTransformerTestCase
{
    public Transformer getTransformer() throws Exception
    {
        Scriptable script = new Scriptable(muleContext);
        script.setScriptEngineName("groovy");
        script.setScriptFile("StringToList.groovy");
        script.initialise();
        
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setName("StringToList");
        transformer.setMuleContext(muleContext);
        transformer.setScript(script);
        transformer.initialise();
        return transformer;
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        Scriptable script = new Scriptable(muleContext);
        script.setScriptFile("ListToString.groovy");
        script.initialise();
        
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setName("ListToString");
        transformer.setMuleContext(muleContext);
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
        List<String> list = new ArrayList<String>(3);
        list.add("this");
        list.add("is");
        list.add("groovy!");
        return list;
    }
}
