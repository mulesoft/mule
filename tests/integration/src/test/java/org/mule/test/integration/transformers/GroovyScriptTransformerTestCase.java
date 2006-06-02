/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.script.ScriptTransformer;
import org.mule.umo.transformer.UMOTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GroovyScriptTransformerTestCase extends AbstractTransformerTestCase
{
    public UMOTransformer getTransformer() throws Exception
    {
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setScriptEngineName("groovy");
        transformer.setName("StringToList");
        transformer.setScriptFile("org/mule/test/integration/transformers/StringToList2.groovy");
        transformer.initialise();
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        ScriptTransformer transformer = new ScriptTransformer();
        transformer.setName("ListToStringTransformer");
        transformer.setScriptFile("org/mule/test/integration/transformers/ListToString2.groovy");
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
