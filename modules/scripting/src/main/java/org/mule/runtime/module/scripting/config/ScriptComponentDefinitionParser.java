/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


