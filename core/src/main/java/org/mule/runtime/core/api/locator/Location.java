/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.locator;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.join;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.locator.Location.LocationImpl.PARTS_SEPARATOR;

import java.util.LinkedList;
import java.util.List;

/**
 * A location allows to define the position of a certain component in the configuration.
 * 
 * Only components contained within global components with name can be referenced using a {@link Location} instance.
 * 
 * It is expected that the string representation of the object defined the serialized for of the location which consist of the
 * global element name and the parts separated by an slash character.
 * 
 * @since 4.0
 */
public interface Location {

  /**
   * @return the global component name that contains the referenced component.
   */
  String getGlobalComponentName();

  /**
   * @return the parts within the global component that define the location of the component.
   */
  List<String> getParts();

  /**
   * @return a new builder instance.
   */
  static Builder builder() {
    return new LocationBuilder();
  }

  /**
   * A builder to create a {@link Location} object.
   *
   * All {@link Location} instances must be created using this builder. The builder implementation may not be thread safe but it
   * is immutable so each method call in the builder returns a new instance so it can be reused.
   * 
   * @since 4.0
   */
  interface Builder {

    /**
     * Sets the name of the global component. This method must only be called once.
     *
     * @param globalName the name of the global component
     * @return a new builder with the provided configuration.
     */
    Builder globalName(String globalName);

    /**
     * Adds a new part at the end of the location.
     * 
     * @param part the name of the part
     * @return a new builder with the provided configuration.
     */
    Builder addPart(String part);

    /**
     * Adds a new "processors" part at the end of the location.
     * 
     * All component that allow nested processors must have the "processors" as attribute for holding the nested processors.
     * 
     * @return a new builder with the provided configuration.
     */
    Builder addProcessorsPart();

    /**
     * Adds a new index part. The index part is used to reference a component within a collection.
     * 
     * There cannot be two index parts consecutively.
     * 
     * @param index the index of the component.
     * @return a new builder with the provided configuration.
     */
    Builder addIndexPart(int index);

    /**
     * @return a location build with the provided configuration.
     */
    Location build();

  }

  static class LocationImpl implements Location {

    protected static final String PARTS_SEPARATOR = "/";
    private LinkedList<String> parts = new LinkedList<>();

    @Override
    public String getGlobalComponentName() {
      return parts.get(0);
    }

    @Override
    public List<String> getParts() {
      return parts.subList(1, parts.size() - 1);
    }

    @Override
    public String toString() {
      return join(parts, PARTS_SEPARATOR);
    }
  }

  static class LocationBuilder implements Builder {

    private LocationImpl location = new LocationImpl();
    private boolean globalNameAlreadySet = false;

    @Override
    public Builder globalName(String globalName) {
      globalNameAlreadySet = true;
      verifyPartDoesNotContainsSlash(globalName);
      LocationBuilder locationBuilder = builderCopy();
      locationBuilder.location.parts.add(0, globalName);
      return locationBuilder;
    }

    @Override
    public Builder addPart(String part) {
      verifyPartDoesNotContainsSlash(part);
      LocationBuilder locationBuilder = builderCopy();
      locationBuilder.location.parts.addLast(part);
      return locationBuilder;
    }

    @Override
    public Builder addProcessorsPart() {
      LocationBuilder locationBuilder = builderCopy();
      locationBuilder.location.parts.add("processors");
      return locationBuilder;
    }

    @Override
    public Builder addIndexPart(int index) {
      verifyPreviousPartIsNotIndex();
      LocationBuilder locationBuilder = builderCopy();
      locationBuilder.location.parts.addLast(String.valueOf(index));
      return locationBuilder;
    }

    private void verifyPreviousPartIsNotIndex() {
      checkState(!location.parts.isEmpty(), "An index cannot be the first part");
      try {
        parseInt(location.parts.getLast());
        checkState(false, "A location cannot have two consecutive index");
      } catch (NumberFormatException e) {
        // all good, not an index.
      }
    }

    private LocationBuilder builderCopy() {
      LocationBuilder locationBuilder = new LocationBuilder();
      locationBuilder.globalNameAlreadySet = this.globalNameAlreadySet;
      locationBuilder.location.parts.addAll(this.location.parts);
      return locationBuilder;
    }

    private void verifyPartDoesNotContainsSlash(String part) {
      checkArgument(!part.contains(PARTS_SEPARATOR), "Slash cannot be part of the global name or part, bad part is " + part);
    }

    @Override
    public Location build() {
      checkState(globalNameAlreadySet, "global component name must be set");
      return location;
    }
  }


}
