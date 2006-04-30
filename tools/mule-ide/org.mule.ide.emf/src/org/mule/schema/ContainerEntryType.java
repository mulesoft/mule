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
 * A representation of the model object '<em><b>Container Entry Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ContainerEntryType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ContainerEntryType#getReference <em>Reference</em>}</li>
 *   <li>{@link org.mule.schema.ContainerEntryType#isRequired <em>Required</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getContainerEntryType()
 * @model extendedMetaData="name='container-entryType' kind='mixed'"
 * @generated
 */
public interface ContainerEntryType extends EObject {
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
     * @see org.mule.schema.MulePackage#getContainerEntryType_Mixed()
     * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
     *        extendedMetaData="kind='elementWildcard' name=':mixed'"
     * @generated
     */
    FeatureMap getMixed();

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
     * @see org.mule.schema.MulePackage#getContainerEntryType_Reference()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     *        extendedMetaData="kind='attribute' name='reference' namespace='##targetNamespace'"
     * @generated
     */
    String getReference();

    /**
     * Sets the value of the '{@link org.mule.schema.ContainerEntryType#getReference <em>Reference</em>}' attribute.
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
     * @see org.mule.schema.MulePackage#getContainerEntryType_Required()
     * @model default="true" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
     *        extendedMetaData="kind='attribute' name='required' namespace='##targetNamespace'"
     * @generated
     */
    boolean isRequired();

    /**
     * Sets the value of the '{@link org.mule.schema.ContainerEntryType#isRequired <em>Required</em>}' attribute.
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
     * Unsets the value of the '{@link org.mule.schema.ContainerEntryType#isRequired <em>Required</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #isSetRequired()
     * @see #isRequired()
     * @see #setRequired(boolean)
     * @generated
     */
    void unsetRequired();

    /**
     * Returns whether the value of the '{@link org.mule.schema.ContainerEntryType#isRequired <em>Required</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return whether the value of the '<em>Required</em>' attribute is set.
     * @see #unsetRequired()
     * @see #isRequired()
     * @see #setRequired(boolean)
     * @generated
     */
    boolean isSetRequired();

} // ContainerEntryType
