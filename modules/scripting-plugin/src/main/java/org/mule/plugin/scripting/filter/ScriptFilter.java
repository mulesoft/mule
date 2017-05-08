/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.filter;

import static org.mule.plugin.scripting.component.Scriptable.BINDING_MESSAGE;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.plugin.scripting.component.Scriptable;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;

import javax.script.Bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptFilter extends AbstractFilteringMessageProcessor implements Filter, Initialisable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScriptFilter.class);

  private Scriptable script;

  private String name;

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(script, muleContext);
  }

  @Override
  public void dispose() {
    LifecycleUtils.disposeIfNeeded(script, LOGGER);
  }

  @Override
  public boolean accept(Event event, Event.Builder builder) {
    Bindings bindings = script.getScriptEngine().createBindings();

    script.populateBindings(bindings, event, builder);
    try {
      return (Boolean) script.runScript(bindings);
    } catch (Throwable e) {
      // TODO MULE-9356 ScriptFilter should rethrow exceptions, or at least log, not ignore them
      return false;
    } finally {
      builder.message((Message) bindings.get(BINDING_MESSAGE));
    }
  }

  @Override
  public boolean accept(Message message, Event.Builder builder) {
    Bindings bindings = script.getScriptEngine().createBindings();

    // TODO MULE-9341 Remove Filters.
    Flow flow = builder("ScriptFilterFlow", muleContext).build();
    Event event =
        Event.builder(create(flow, fromSingleComponent("ScriptFilter"))).message(message).flow(flow).build();
    script.populateBindings(bindings, event, builder);
    try {
      return (Boolean) script.runScript(bindings);
    } catch (Throwable e) {
      // TODO MULE-9356 ScriptFilter should rethrow exceptions, or at least log, not ignore them
      return false;
    }
  }

  public Scriptable getScript() {
    return script;
  }

  public void setScript(Scriptable script) {
    this.script = script;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}


