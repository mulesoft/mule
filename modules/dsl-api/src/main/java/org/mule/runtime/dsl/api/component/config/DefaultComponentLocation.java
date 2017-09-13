/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.LocationPart;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A component location describes where the component is defined in the configuration of the artifact.
 *
 * For instance:
 * <ul>
 * <li>COMPONENT_NAME - global component defined with name COMPONENT_NAME</li>
 * <li>FLOW_NAME/source - a source defined within a flow</li>
 * <li>FLOW_NAME/processors/0 - the first processor defined within a flow with name FLOW_NAME</li>
 * <li>FLOW_NAME/processors/4/1 - the first processors defined inside another processor which is positioned fifth within a flow
 * with name FLOW_NAME</li>
 * <li>FLOW_NAME/errorHandler/0 - the first on-error within the error handler</li>
 * <li>FLOW_NAME/0/errorHandler/3 - the third on-error within the error handler of the first element of the flow with name
 * FLOW_NAME</li>
 * </ul>
 *
 * The different {@link DefaultLocationPart}s in FLOW_NAME/processors/1 are:
 * <ul>
 * <li>'processors' as partPath and no component identifier since this part is synthetic to indicate the part of the flow
 * referenced by the next index</li>
 * <li>'1' as partPath and 'mule:payload' as component identifier assuming that the second processor of the flow was a set-payload
 * component</li>
 * </ul>
 *
 * @since 4.0
 */
public class DefaultComponentLocation implements ComponentLocation, Serializable {

  private static final long serialVersionUID = 4958158607813720623L;

  private String name;
  private LinkedList<DefaultLocationPart> parts;
  private volatile String location;

  /**
   * Creates a virtual {@link ComponentLocation} for a single element, using the core namespace and using UNKNOWN as type. Only
   * meant for situations where a real location cannot be obtained.
   *
   * @param component the name of the element
   * @return a location for it
   */
  public static DefaultComponentLocation fromSingleComponent(String component) {
    DefaultLocationPart part = new DefaultLocationPart(component,
                                                       of(TypedComponentIdentifier.builder()
                                                           .type(UNKNOWN)
                                                           .identifier(ComponentIdentifier
                                                               .buildFromStringRepresentation(component))
                                                           .build()),
                                                       empty(),
                                                       empty());
    return new DefaultComponentLocation(of(component), asList(part));
  }

  /**
   * @param name the name of the global element in which the specific component is located.
   * @param parts the set of parts to locate the component.
   */
  public DefaultComponentLocation(Optional<String> name, List<DefaultLocationPart> parts) {
    this.name = name.orElse(null);
    this.parts = new LinkedList<>(parts);
  }

  /**
   * {@inheritDoc}
   */
  public Optional<String> getName() {
    return ofNullable(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<LocationPart> getParts() {
    return unmodifiableList(parts);
  }

  @Override
  public TypedComponentIdentifier getComponentIdentifier() {
    return parts.get(parts.size() - 1).getPartIdentifier().get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getFileName() {
    return parts.getLast().getFileName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Integer> getLineInFile() {
    return parts.getLast().getLineInFile();
  }

  /**
   * @return a string representation of the {@link DefaultComponentLocation}.
   */
  @Override
  public String getLocation() {
    if (location == null) {
      synchronized (this) {
        if (location == null) {
          StringBuilder locationBuilder = new StringBuilder();
          for (DefaultLocationPart part : parts) {
            locationBuilder.append("/").append(part.getPartPath());
          }
          location = locationBuilder.replace(0, 1, "").toString();
        }
      }
    }
    return location;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRootContainerName() {
    return getParts().get(0).getPartPath();
  }

  /**
   * Creates a new instance of ComponentLocation adding the specified part.
   *
   * @param partPath the path of this part
   * @param partIdentifier the component identifier of the part if it's not a synthetic part
   * @return a new instance with the given location part appended.
   */
  public DefaultComponentLocation appendLocationPart(String partPath, Optional<TypedComponentIdentifier> partIdentifier,
                                                     Optional<String> fileName, Optional<Integer> lineInFile) {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart(partPath, partIdentifier, fileName, lineInFile)).build());
  }

  /**
   * Utility method that adds a processors part to the location. This is the part used for nested processors in configuration
   * components.
   *
   * @return a new instance with the processors location part appended.
   */
  public DefaultComponentLocation appendProcessorsPart() {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart("processors", empty(), empty(), empty())).build());
  }

  /**
   * Utility method that adds a router part to the location. This is the part used for nested processors in configuration
   * components.
   *
   * @return a new instance with the processors location part appended.
   */
  public DefaultComponentLocation appendRoutePart() {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart("route", empty(), empty(), empty())).build());
  }

  /**
   * A location part represent an specific location of a component within another component.
   *
   * @since 4.0
   */
  public static class DefaultLocationPart implements LocationPart, Serializable {

    private static final long serialVersionUID = 5757545892752260058L;

    private String partPath;
    private TypedComponentIdentifier partIdentifier;
    private String fileName;
    private Integer lineInFile;

    /**
     * @param partPath the path of this part
     * @param partIdentifier the component identifier of the part if it's not a synthetic part
     * @param fileName the file name in which the component was defined
     * @param lineInFile the line number in which the component was defined
     */
    public DefaultLocationPart(String partPath, Optional<TypedComponentIdentifier> partIdentifier, Optional<String> fileName,
                               Optional<Integer> lineInFile) {
      this.partPath = partPath;
      this.partIdentifier = partIdentifier.orElse(null);
      fileName.ifPresent(configFileName -> this.fileName = configFileName);
      lineInFile.ifPresent(line -> this.lineInFile = line);
    }

    /**
     * @return the string representation of the part
     */
    @Override
    public String getPartPath() {
      return partPath;
    }

    /**
     * @return if it's a synthetic part this is null, if not then it's the identifier of the configuration element.
     */
    @Override
    public Optional<TypedComponentIdentifier> getPartIdentifier() {
      return ofNullable(partIdentifier);
    }

    @Override
    public Optional<String> getFileName() {
      return ofNullable(fileName);
    }

    @Override
    public Optional<Integer> getLineInFile() {
      return ofNullable(lineInFile);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DefaultLocationPart that = (DefaultLocationPart) o;

      if (!Objects.equals(getPartPath(), that.getPartPath())) {
        return false;
      }
      if (getPartIdentifier() != null ? !getPartIdentifier().equals(that.getPartIdentifier())
          : that.getPartIdentifier() != null) {
        return false;
      }
      if (getFileName() != null ? !getFileName().equals(that.getFileName()) : that.getFileName() != null) {
        return false;
      }
      return getLineInFile() != null ? getLineInFile().equals(that.getLineInFile()) : that.getLineInFile() == null;
    }

    @Override
    public int hashCode() {
      int result = getPartPath() != null ? getPartPath().hashCode() : 31;
      result = 31 * result + (getPartIdentifier() != null ? getPartIdentifier().hashCode() : 0);
      result = 31 * result + (getFileName() != null ? getFileName().hashCode() : 0);
      result = 31 * result + (getLineInFile() != null ? getLineInFile().hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "DefaultLocationPart{" +
          "partPath='" + partPath + '\'' +
          ", partIdentifier=" + partIdentifier +
          ", fileName='" + fileName + '\'' +
          ", lineInFile=" + lineInFile +
          '}';
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultComponentLocation that = (DefaultComponentLocation) o;

    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
      return false;
    }
    if (!getParts().equals(that.getParts())) {
      return false;
    }
    return getLocation() != null ? getLocation().equals(that.getLocation()) : that.getLocation() == null;
  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + getParts().hashCode();
    result = 31 * result + (getLocation() != null ? getLocation().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DefaultComponentLocation{" +
        "name='" + name + '\'' +
        ", parts=" + parts +
        ", location='" + getLocation() + '\'' +
        '}';
  }
}
