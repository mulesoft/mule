/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Container Property Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ContainerPropertyType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ContainerPropertyType#getContainer <em>Container</em>}</li>
 *   <li>{@link org.mule.schema.ContainerPropertyType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.ContainerPropertyType#getReference <em>Reference</em>}</li>
 *   <li>{@link org.mule.schema.ContainerPropertyType#isRequired <em>Required</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getContainerPropertyType()
 * @model extendedMetaData="name='container-propertyType' kind='mixed'"
 * @generated
 */
public interface ContainerPropertyType extends EObject {
    /**
     * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
     * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Mixed</em>' attribute list.
     * @see org.mule.schema.MulePackage#getContainerPropertyType_Mixed()
     * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
     *        extendedMetaData="kind='elementWildcard' name=':mixed'"
     * @generated
     */
    FeatureMap getMixed();

    /**
     * Returns the value of the '<em><b>Container</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Container</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Container</em>' attribute.
     * @see #setContainer(String)
     * @see org.mule.schema.MulePackage#getContainerPropertyType_Container()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='container' namespace='##targetNamespace'"
     * @generated
     */
    String getContainer();

    /**
     * Sets the value of the '{@link org.mule.schema.ContainerPropertyType#getContainer <em>Container</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Container</em>' attribute.
     * @see #getContainer()
     * @generated
     */
    void setContainer(String value);

    /**
     * Returns the value of the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Name</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Name</em>' attribute.
     * @see #setName(String)
     * @see org.mule.schema.MulePackage#getContainerPropertyType_Name()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
     * @generated
     */
    String getName();

    /**
     * Sets the value of the '{@link org.mule.schema.ContainerPropertyType#getName <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Name</em>' attribute.
     * @see #getName()
     * @generated
     */
    void setName(String value);

    /**
     * Returns the value of the '<em><b>Reference</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Reference</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Reference</em>' attribute.
     * @see #setReference(String)
     * @see org.mule.schema.MulePackage#getContainerPropertyType_Reference()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     *        extendedMetaData="kind='attribute' name='reference' namespace='##targetNamespace'"
     * @generated
     */
    String getReference();

    /**
     * Sets the value of the '{@link org.mule.schema.ContainerPropertyType#getReference <em>Reference</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Reference</em>' attribute.
     * @see #getReference()
     * @generated
     */
    void setReference(String value);

    /**
     * Returns the value of the '<em><b>Required</b></em>' attribute.
     * The default value is <code>"true"</code>.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Required</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Required</em>' attribute.
     * @see #isSetRequired()
     * @see #unsetRequired()
     * @see #setRequired(boolean)
     * @see org.mule.schema.MulePackage#getContainerPropertyType_Required()
     * @model default="true" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
     *        extendedMetaData="kind='attribute' name='required' namespace='##targetNamespace'"
     * @generated
     */
    boolean isRequired();

    /**
     * Sets the value of the '{@link org.mule.schema.ContainerPropertyType#isRequired <em>Required</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Required</em>' attribute.
     * @see #isSetRequired()
     * @see #unsetRequired()
     * @see #isRequired()
     * @generated
     */
    void setRequired(boolean value);

    /**
     * Unsets the value of the '{@link org.mule.schema.ContainerPropertyType#isRequired <em>Required</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #isSetRequired()
     * @see #isRequired()
     * @see #setRequired(boolean)
     * @generated
     */
    void unsetRequired();

    /**
     * Returns whether the value of the '{@link org.mule.schema.ContainerPropertyType#isRequired <em>Required</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return whether the value of the '<em>Required</em>' attribute is set.
     * @see #unsetRequired()
     * @see #isRequired()
     * @see #setRequired(boolean)
     * @generated
     */
    boolean isSetRequired();

} // ContainerPropertyType
