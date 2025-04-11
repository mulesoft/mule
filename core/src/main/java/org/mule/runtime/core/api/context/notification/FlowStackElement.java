/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.mule.runtime.api.component.Component.Annotations.SOURCE_LOCATION_ANNOTATION_KEY;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;

import java.io.Serializable;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Keeps context information about the processors that a flow executed.
 *
 * @since 3.8.0
 */
@NoInstantiate
@NoExtend
public class FlowStackElement implements Serializable {

  private static final long serialVersionUID = -2372094725681872367L;

  private final String flowName;
  private final String processorPath;
  private final ComponentLocation executingLocation;
  private final String executingComponentSourceLocation;
  private final long creationTime;
  private final transient ComponentIdentifier chainIdentifier;

  public FlowStackElement(String flowName, String processorPath, ComponentLocation executingLocation,
                          Map<QName, Object> executingComponentAnnotations) {
    this(flowName, null, processorPath, executingLocation, executingComponentAnnotations);
  }

  public FlowStackElement(String flowName, ComponentIdentifier chainIdentifier, String processorPath,
                          ComponentLocation executingLocation, Map<QName, Object> executingComponentAnnotations) {
    this.flowName = flowName;
    this.processorPath = processorPath;
    this.executingLocation = requireNonNull(executingLocation);
    this.executingComponentSourceLocation = (String) executingComponentAnnotations.get(SOURCE_LOCATION_ANNOTATION_KEY);
    this.creationTime = now().toEpochMilli();
    this.chainIdentifier = chainIdentifier;
  }

  /**
   * @return the path of the currently executing processor in the flow represented by this element.
   * @deprecated Use {@link #getExecutingLocation()} and {@link #getExecutingComponentAnnotations()} instead.
   */
  @Deprecated
  public String getProcessorPath() {
    return processorPath;
  }

  /**
   * @return the name of the flow which execution is represented by this element.
   */
  public String getFlowName() {
    return flowName;
  }

  /**
   * @return the component identifier of the chain which execution is represented by {@link #getFlowName()}.
   */
  public ComponentIdentifier getChainIdentifier() {
    return chainIdentifier;
  }

  /**
   * @return the location of the component on this execution point.
   * @since 4.10
   */
  public ComponentLocation getExecutingLocation() {
    return executingLocation;
  }

  /**
   * @return the location within the artifact source for the component on this execution point.
   * @since 4.10
   */
  public String getExecutingComponentSourceLocation() {
    return executingComponentSourceLocation;
  }

  /**
   * @return the time when the flow stack was created.
   */
  public long getCreationTimeLong() {
    return creationTime;
  }

  /**
   * @return the milliseconds elapsed between its creation and now.
   */
  public long getElapsedTimeLong() {
    return now().toEpochMilli() - creationTime;
  }

  /**
   * @return the time when the flow stack was created.
   *
   * @deprecated Use {@link #getCreationTimeLong()} instead.
   */
  @Deprecated
  public Long getCreationTime() {
    return creationTime;
  }

  /**
   * @return the milliseconds elapsed between its creation and now.
   *
   * @deprecated Use {@link #getElapsedTimeLong()} instead.
   */
  @Deprecated
  public Long getElapsedTime() {
    return currentTimeMillis() - creationTime;
  }

  @Override
  public String toString() {
    if (processorPath == null) {
      return flowName;
    } else {
      return flowName.concat("(").concat(processorPath).concat(")");
    }
  }

  public String toStringWithElapsedTime() {
    return toString().concat(" ").concat(Long.toString(getElapsedTimeLong())).concat(" ms");
  }

  public String toStringEventDumpFormat() {
    return getExecutingLocation().getComponentIdentifier().getIdentifier().toString().concat("@")
        .concat(getExecutingLocation().getLocation())
        .concat("(" + getExecutingComponentSourceLocation() + ")")
        .concat(" ").concat(Long.toString(getElapsedTimeLong())).concat(" ms");
  }
}
