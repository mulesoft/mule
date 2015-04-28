/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.2-hudson-jaxb-ri-2.2-63- 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.06.14 at 03:58:12 PM GMT-03:00 
//


package org.mule.module.extension.internal.capability.xml.schema.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for extensionType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="extensionType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.w3.org/2001/XMLSchema}annotated">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.w3.org/2001/XMLSchema}typeDefParticle" minOccurs="0"/>
 *         &lt;group ref="{http://www.w3.org/2001/XMLSchema}attrDecls"/>
 *       &lt;/sequence>
 *       &lt;attribute name="base" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extensionType", propOrder = {
        "group",
        "all",
        "choice",
        "sequence",
        "attributeOrAttributeGroup",
        "anyAttribute"
})
@XmlSeeAlso({
                    SimpleExtensionType.class
            })
public class ExtensionType
        extends Annotated
{

    protected GroupRef group;
    protected All all;
    protected ExplicitGroup choice;
    protected ExplicitGroup sequence;
    @XmlElements({
                         @XmlElement(name = "attributeGroup", type = AttributeGroupRef.class),
                         @XmlElement(name = "attribute", type = Attribute.class)
                 })
    protected List<Attribute> attributeOrAttributeGroup;
    protected Wildcard anyAttribute;
    @XmlAttribute(name = "base", required = true)
    protected QName base;

    /**
     * Gets the value of the group property.
     *
     * @return possible object is
     * {@link GroupRef }
     */
    public GroupRef getGroup()
    {
        return group;
    }

    /**
     * Sets the value of the group property.
     *
     * @param value allowed object is
     *              {@link GroupRef }
     */
    public void setGroup(GroupRef value)
    {
        this.group = value;
    }

    /**
     * Gets the value of the all property.
     *
     * @return possible object is
     * {@link All }
     */
    public All getAll()
    {
        return all;
    }

    /**
     * Sets the value of the all property.
     *
     * @param value allowed object is
     *              {@link All }
     */
    public void setAll(All value)
    {
        this.all = value;
    }

    /**
     * Gets the value of the choice property.
     *
     * @return possible object is
     * {@link ExplicitGroup }
     */
    public ExplicitGroup getChoice()
    {
        return choice;
    }

    /**
     * Sets the value of the choice property.
     *
     * @param value allowed object is
     *              {@link ExplicitGroup }
     */
    public void setChoice(ExplicitGroup value)
    {
        this.choice = value;
    }

    /**
     * Gets the value of the sequence property.
     *
     * @return possible object is
     * {@link ExplicitGroup }
     */
    public ExplicitGroup getSequence()
    {
        return sequence;
    }

    /**
     * Sets the value of the sequence property.
     *
     * @param value allowed object is
     *              {@link ExplicitGroup }
     */
    public void setSequence(ExplicitGroup value)
    {
        this.sequence = value;
    }

    /**
     * Gets the value of the attributeOrAttributeGroup property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attributeOrAttributeGroup property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttributeOrAttributeGroup().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link AttributeGroupRef }
     * {@link Attribute }
     */
    public List<Attribute> getAttributeOrAttributeGroup()
    {
        if (attributeOrAttributeGroup == null)
        {
            attributeOrAttributeGroup = new ArrayList<Attribute>();
        }
        return this.attributeOrAttributeGroup;
    }

    /**
     * Gets the value of the anyAttribute property.
     *
     * @return possible object is
     * {@link Wildcard }
     */
    public Wildcard getAnyAttribute()
    {
        return anyAttribute;
    }

    /**
     * Sets the value of the anyAttribute property.
     *
     * @param value allowed object is
     *              {@link Wildcard }
     */
    public void setAnyAttribute(Wildcard value)
    {
        this.anyAttribute = value;
    }

    /**
     * Gets the value of the base property.
     *
     * @return possible object is
     * {@link QName }
     */
    public QName getBase()
    {
        return base;
    }

    /**
     * Sets the value of the base property.
     *
     * @param value allowed object is
     *              {@link QName }
     */
    public void setBase(QName value)
    {
        this.base = value;
    }

}
