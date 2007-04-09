/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.script;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;

import java.util.ArrayList;
import java.util.List;

public class GroovyScriptTransformerTestCase extends AbstractTransformerTestCase
{

    public UMOTransformer getTransformer() throws Exception
    {
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setScriptEngineName("groovy");
        transformer.setName("StringToList");
        transformer.setScriptFile("StringToList2.groovy");
        transformer.initialise();
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setName("ListToStringTransformer");
        transformer.setScriptFile("ListToString2.groovy");
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

    // @Override
    protected void doTestClone(UMOTransformer original, UMOTransformer clone) throws Exception
    {
        super.doTestClone(original, clone);

        ScriptTransformer t1 = (ScriptTransformer) original;
        ScriptTransformer t2 = (ScriptTransformer) clone;

        // The Scriptable instance must be different 
        assertNotSame(t1.scriptable, t2.scriptable);
    }

}
