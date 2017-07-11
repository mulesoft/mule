/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.api.exception.MuleException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Orange implements OrangeInterface {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 2556604671068150589L;

  private boolean bitten = false;
  private Integer segments = new Integer(10);
  private Double radius = new Double(4.34);
  private String brand = "Pirulo";

  private FruitCleaner cleaner;

  private Map mapProperties;

  private List listProperties;

  private List arrayProperties;

  public Orange() {
    super();
  }

  public Orange(Integer segments, Double radius, String brand) {
    super();
    this.segments = segments;
    this.radius = radius;
    this.brand = brand;
  }

  public Orange(HashMap<String, Object> props) throws MuleException {
    setBrand((String) props.get("brand"));
    setRadius((Double) props.get("radius"));
    setSegments((Integer) props.get("segments"));
  }

  @Override
  public void bite() {
    bitten = true;
  }

  @Override
  public boolean isBitten() {
    return bitten;
  }

  @Override
  public String getBrand() {
    return brand;
  }

  @Override
  public Integer getSegments() {
    return segments;
  }

  @Override
  public Double getRadius() {
    return radius;
  }

  @Override
  public void setBrand(String string) {
    brand = string;
  }

  @Override
  public void setSegments(Integer integer) {
    segments = integer;
  }

  @Override
  public void setRadius(Double double1) {
    radius = double1;
  }

  /**
   * @return Returns the listProperties.
   */
  @Override
  public List getListProperties() {
    return listProperties;
  }

  /**
   * @param listProperties The listProperties to set.
   */
  @Override
  public void setListProperties(List listProperties) {
    this.listProperties = listProperties;
  }

  /**
   * @return Returns the mapProperties.
   */
  @Override
  public Map getMapProperties() {
    return mapProperties;
  }

  /**
   * @param mapProperties The mapProperties to set.
   */
  @Override
  public void setMapProperties(Map mapProperties) {
    this.mapProperties = mapProperties;
  }

  /**
   * @return Returns the arrayProperties.
   */
  @Override
  public List getArrayProperties() {
    return arrayProperties;
  }

  /**
   * @param arrayProperties The arrayProperties to set.
   */
  @Override
  public void setArrayProperties(List arrayProperties) {
    this.arrayProperties = arrayProperties;
  }

  @Override
  public FruitCleaner getCleaner() {
    return cleaner;
  }

  @Override
  public void setCleaner(FruitCleaner cleaner) {
    this.cleaner = cleaner;
  }

  @Override
  public void wash() {
    cleaner.wash(this);
  }

  @Override
  public void polish() {
    cleaner.polish(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (bitten ? 1231 : 1237);
    result = prime * result + ((brand == null) ? 0 : brand.hashCode());
    result = prime * result + ((radius == null) ? 0 : radius.hashCode());
    result = prime * result + ((segments == null) ? 0 : segments.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Orange other = (Orange) obj;
    if (bitten != other.bitten) {
      return false;
    }
    if (brand == null) {
      if (other.brand != null) {
        return false;
      }
    } else if (!brand.equals(other.brand)) {
      return false;
    }
    if (radius == null) {
      if (other.radius != null) {
        return false;
      }
    } else if (!radius.equals(other.radius)) {
      return false;
    }
    if (segments == null) {
      if (other.segments != null) {
        return false;
      }
    } else if (!segments.equals(other.segments)) {
      return false;
    }
    return true;
  }
}
