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
 * A representation of the model object '<em><b>Response Router Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ResponseRouterType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ResponseRouterType#getEndpoint <em>Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.ResponseRouterType#getGlobalEndpoint <em>Global Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.ResponseRouterType#getRouter <em>Router</em>}</li>
 *   <li>{@link org.mule.schema.ResponseRouterType#getTimeout <em>Timeout</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getResponseRouterType()
 * @model extendedMetaData="name='response-routerType' kind='mixed'"
 * @generated
 */
public interface ResponseRouterType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getResponseRouterType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Endpoint</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.EndpointType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Endpoint</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Endpoint</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getResponseRouterType_Endpoint()
	 * @model type="org.mule.schema.EndpointType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='endpoint' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getEndpoint();

	/**
	 * Returns the value of the '<em><b>Global Endpoint</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.GlobalEndpointType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Global Endpoint</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Global Endpoint</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getResponseRouterType_GlobalEndpoint()
	 * @model type="org.mule.schema.GlobalEndpointType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='global-endpoint' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getGlobalEndpoint();

	/**
	 * Returns the value of the '<em><b>Router</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.RouterType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Router</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Router</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getResponseRouterType_Router()
	 * @model type="org.mule.schema.RouterType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='router' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getRouter();

	/**
	 * Returns the value of the '<em><b>Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Timeout</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timeout</em>' attribute.
	 * @see #setTimeout(String)
	 * @see org.mule.schema.MulePackage#getResponseRouterType_Timeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='timeout' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTimeout();

	/**
	 * Sets the value of the '{@link org.mule.schema.ResponseRouterType#getTimeout <em>Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timeout</em>' attribute.
	 * @see #getTimeout()
	 * @generated
	 */
	void setTimeout(String value);

} // ResponseRouterType
