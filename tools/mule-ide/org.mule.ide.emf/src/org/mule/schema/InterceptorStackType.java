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
 * A representation of the model object '<em><b>Interceptor Stack Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.InterceptorStackType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.InterceptorStackType#getInterceptor <em>Interceptor</em>}</li>
 *   <li>{@link org.mule.schema.InterceptorStackType#getName <em>Name</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getInterceptorStackType()
 * @model extendedMetaData="name='interceptor-stackType' kind='mixed'"
 * @generated
 */
public interface InterceptorStackType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getInterceptorStackType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Interceptor</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.InterceptorType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Interceptor</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Interceptor</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getInterceptorStackType_Interceptor()
	 * @model type="org.mule.schema.InterceptorType" containment="true" resolveProxies="false" required="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='interceptor' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getInterceptor();

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
	 * @see org.mule.schema.MulePackage#getInterceptorStackType_Name()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.mule.schema.InterceptorStackType#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

} // InterceptorStackType
