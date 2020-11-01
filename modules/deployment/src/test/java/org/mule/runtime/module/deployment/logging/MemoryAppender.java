/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.logging;

import static org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout;
import static org.apache.logging.log4j.core.util.Booleans.parseBoolean;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MemoryAppender extends AbstractAppender {

  private List<LogEvent> events;

  protected MemoryAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions);
    this.events = new ArrayList<>();
  }

  @Override
  public void append(LogEvent event) {
    this.events.add(event.toImmutable());
  }

  @Override
  public void stop() {
    super.stop();
    this.events.clear();
  }

  public Stream<String> getLogLines() {
    if (events == null) {
      return new ArrayList<String>(0).stream();
    }

    return events.stream().filter(Objects::nonNull).map(((StringLayout) getLayout())::toSerializable);
  }

  public static MemoryAppender createAppender(@PluginElement("Layout") Layout<? extends Serializable> layout,
                                              @PluginElement("Filter") final Filter filter,
                                              @PluginAttribute("name") final String name,
                                              @PluginAttribute(value = "ignoreExceptions",
                                                  defaultBoolean = true) final String ignore) {
    if (name == null) {
      LOGGER.error("No name provided for MemoryAppender");
      return null;
    }

    if (layout == null) {
      layout = createDefaultLayout();
    }

    final boolean ignoreExceptions = parseBoolean(ignore, true);
    return new MemoryAppender(name, filter, layout, ignoreExceptions);
  }
}
