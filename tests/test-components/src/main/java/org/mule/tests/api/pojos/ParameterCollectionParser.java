/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api.pojos;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TypeDsl(allowTopLevelDefinition = true)
public class ParameterCollectionParser {

  @Parameter
  @Optional
  private String firstname;

  @Parameter
  @Optional
  private String lastname;

  @Parameter
  @Optional
  private int age;

  @Parameter
  @Optional
  private ParameterCollectionParser firstChild;

  @Parameter
  @Optional
  private ParameterCollectionParser secondChild;

  @Parameter
  @Optional
  private List<ParameterCollectionParser> otherChildren;

  @Parameter
  @Optional
  private LinkedList<ParameterCollectionParser> otherChildrenCustomCollectionType;

  @Parameter
  @Optional
  private List<String> simpleTypeChildList;

  @Parameter
  @Optional
  private Set<String> simpleTypeChildSet;

  @Parameter
  @Optional
  private List<String> otherSimpleTypeChildList;

  @Parameter
  @Optional
  private Map<String, Integer> simpleTypeEntry;

  @Parameter
  @Optional
  private Map<Long, ParameterCollectionParser> complexTypeEntry;

  @Parameter
  @Optional
  private Map<String, List<String>> simpleListTypeEntry;

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public ParameterCollectionParser getFirstChild() {
    return firstChild;
  }

  public void setFirstChild(ParameterCollectionParser firstChild) {
    this.firstChild = firstChild;
  }

  public ParameterCollectionParser getSecondChild() {
    return secondChild;
  }

  public void setSecondChild(ParameterCollectionParser secondChild) {
    this.secondChild = secondChild;
  }

  public List<ParameterCollectionParser> getOtherChildren() {
    return otherChildren;
  }

  public void setOtherChildren(List<ParameterCollectionParser> otherChildren) {
    this.otherChildren = otherChildren;
  }

  public LinkedList<ParameterCollectionParser> getOtherChildrenCustomCollectionType() {
    return otherChildrenCustomCollectionType;
  }

  public void setOtherChildrenCustomCollectionType(LinkedList<ParameterCollectionParser> otherChildrenCustomCollectionType) {
    this.otherChildrenCustomCollectionType = otherChildrenCustomCollectionType;
  }

  public List<String> getSimpleTypeChildList() {
    return simpleTypeChildList;
  }

  public void setSimpleTypeChildList(List<String> simpleTypeChildList) {
    this.simpleTypeChildList = simpleTypeChildList;
  }

  public Set<String> getSimpleTypeChildSet() {
    return simpleTypeChildSet;
  }

  public void setSimpleTypeChildSet(Set<String> simpleTypeChildSet) {
    this.simpleTypeChildSet = simpleTypeChildSet;
  }

  public List<String> getOtherSimpleTypeChildList() {
    return otherSimpleTypeChildList;
  }

  public void setOtherSimpleTypeChildList(List<String> otherSimpleTypeChildList) {
    this.otherSimpleTypeChildList = otherSimpleTypeChildList;
  }

  public Map<String, Integer> getSimpleTypeEntry() {
    return simpleTypeEntry;
  }

  public void setSimpleTypeEntry(Map<String, Integer> simpleTypeEntry) {
    this.simpleTypeEntry = simpleTypeEntry;
  }

  public Map<Long, ParameterCollectionParser> getComplexTypeEntry() {
    return complexTypeEntry;
  }

  public void setComplexTypeEntry(Map<Long, ParameterCollectionParser> complexTypeEntry) {
    this.complexTypeEntry = complexTypeEntry;
  }

  public Map<String, List<String>> getSimpleListTypeEntry() {
    return simpleListTypeEntry;
  }

  public void setSimpleListTypeEntry(Map<String, List<String>> simpleListTypeEntry) {
    this.simpleListTypeEntry = simpleListTypeEntry;
  }
}
