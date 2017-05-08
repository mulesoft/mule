/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.config;

import org.mule.plugin.scripting.component.Scriptable;
import org.mule.plugin.scripting.transformer.ScriptTransformer;
import org.mule.runtime.config.spring.dsl.processor.TransformerConfigurator;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.util.Map;


/**
 * {@link TransformerConfigurator} for scripting transformer. Sets the script to be used.
 *
 * @since 4.0
 */
public class ScriptingTransformerConfigurator extends TransformerConfigurator {

  @Override
  public void configure(AbstractTransformer transformerInstance, Map<String, Object> parameters) {
    super.configure(transformerInstance, parameters);
    ((ScriptTransformer) transformerInstance).setScript((Scriptable) parameters.get("script"));
  }
}
