/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Security Manager Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.SecurityManagerType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.SecurityManagerType#getSecurityProvider <em>Security Provider</em>}</li>
 *   <li>{@link org.mule.schema.SecurityManagerType#getEncryptionStrategy <em>Encryption Strategy</em>}</li>
 *   <li>{@link org.mule.schema.SecurityManagerType#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.SecurityManagerType#getRef <em>Ref</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getSecurityManagerType()
 * @model extendedMetaData="name='security-managerType' kind='mixed'"
 * @generated
 */
public interface SecurityManagerType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getSecurityManagerType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Security Provider</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.SecurityProviderType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Security Provider</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Security Provider</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getSecurityManagerType_SecurityProvider()
	 * @model type="org.mule.schema.SecurityProviderType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='security-provider' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getSecurityProvider();

	/**
	 * Returns the value of the '<em><b>Encryption Strategy</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.EncryptionStrategyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Encryption Strategy</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Encryption Strategy</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getSecurityManagerType_EncryptionStrategy()
	 * @model type="org.mule.schema.EncryptionStrategyType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='encryption-strategy' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getEncryptionStrategy();

	/**
	 * Returns the value of the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Class Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Class Name</em>' attribute.
	 * @see #setClassName(String)
	 * @see org.mule.schema.MulePackage#getSecurityManagerType_ClassName()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='className' namespace='##targetNamespace'"
	 * @generated
	 */
	String getClassName();

	/**
	 * Sets the value of the '{@link org.mule.schema.SecurityManagerType#getClassName <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class Name</em>' attribute.
	 * @see #getClassName()
	 * @generated
	 */
	void setClassName(String value);

	/**
	 * Returns the value of the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ref</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ref</em>' attribute.
	 * @see #setRef(String)
	 * @see org.mule.schema.MulePackage#getSecurityManagerType_Ref()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='ref' namespace='##targetNamespace'"
	 * @generated
	 */
	String getRef();

	/**
	 * Sets the value of the '{@link org.mule.schema.SecurityManagerType#getRef <em>Ref</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ref</em>' attribute.
	 * @see #getRef()
	 * @generated
	 */
	void setRef(String value);

} // SecurityManagerType
