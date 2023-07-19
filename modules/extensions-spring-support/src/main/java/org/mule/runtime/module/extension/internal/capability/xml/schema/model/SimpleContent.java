/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.2-hudson-jaxb-ri-2.2-63- 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.14 at 03:58:12 PM GMT-03:00 
//


package org.mule.runtime.module.extension.internal.capability.xml.schema.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java class for anonymous complex type.
 * <p/>
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated">
 *       &lt;choice>
 *         &lt;element name="restriction" type="{http://www.w3.org/2001/XMLSchema}simpleRestrictionType"/>
 *         &lt;element name="extension" type="{http://www.w3.org/2001/XMLSchema}simpleExtensionType"/>
 *       &lt;/choice>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"restriction", "extension"})
@XmlRootElement(name = "simpleContent")
public class SimpleContent extends Annotated {

  protected SimpleRestrictionType restriction;
  protected SimpleExtensionType extension;

  /**
   * Gets the value of the restriction property.
   *
   * @return possible object is {@link SimpleRestrictionType }
   */
  public SimpleRestrictionType getRestriction() {
    return restriction;
  }

  /**
   * Sets the value of the restriction property.
   *
   * @param value allowed object is {@link SimpleRestrictionType }
   */
  public void setRestriction(SimpleRestrictionType value) {
    this.restriction = value;
  }

  /**
   * Gets the value of the extension property.
   *
   * @return possible object is {@link SimpleExtensionType }
   */
  public SimpleExtensionType getExtension() {
    return extension;
  }

  /**
   * Sets the value of the extension property.
   *
   * @param value allowed object is {@link SimpleExtensionType }
   */
  public void setExtension(SimpleExtensionType value) {
    this.extension = value;
  }

}
