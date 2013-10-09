/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.config;

import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.module.scripting.component.ScriptComponent;

public class ScriptComponentDefinitionParser extends ComponentDefinitionParser
{
    public ScriptComponentDefinitionParser()
    {
        super(ScriptComponent.class);
        addAlias("script", "scriptText");
        addAlias("engine", "scriptEngineName");
    }
}


