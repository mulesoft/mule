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
 * A representation of the model object '<em><b>Global Endpoint Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getTransaction <em>Transaction</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getSecurityFilter <em>Security Filter</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getAddress <em>Address</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#isRemoteSync <em>Remote Sync</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getResponseTransformers <em>Response Transformers</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.GlobalEndpointType#getTransformers <em>Transformers</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getGlobalEndpointType()
 * @model extendedMetaData="name='global-endpointType' kind='mixed'"
 * @generated
 */
public interface GlobalEndpointType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Transaction</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transaction</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transaction</em>' containment reference.
	 * @see #setTransaction(TransactionType)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Transaction()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='transaction' namespace='##targetNamespace'"
	 * @generated
	 */
	TransactionType getTransaction();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getTransaction <em>Transaction</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transaction</em>' containment reference.
	 * @see #getTransaction()
	 * @generated
	 */
	void setTransaction(TransactionType value);

	/**
	 * Returns the value of the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Filter</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Filter</em>' containment reference.
	 * @see #setFilter(FilterType)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Filter()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='filter' namespace='##targetNamespace'"
	 * @generated
	 */
	FilterType getFilter();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getFilter <em>Filter</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Filter</em>' containment reference.
	 * @see #getFilter()
	 * @generated
	 */
	void setFilter(FilterType value);

	/**
	 * Returns the value of the '<em><b>Security Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Security Filter</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Security Filter</em>' containment reference.
	 * @see #setSecurityFilter(SecurityFilterType)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_SecurityFilter()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='security-filter' namespace='##targetNamespace'"
	 * @generated
	 */
	SecurityFilterType getSecurityFilter();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getSecurityFilter <em>Security Filter</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Security Filter</em>' containment reference.
	 * @see #getSecurityFilter()
	 * @generated
	 */
	void setSecurityFilter(SecurityFilterType value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.PropertiesType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Properties</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Properties()
	 * @model type="org.mule.schema.PropertiesType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getProperties();

	/**
	 * Returns the value of the '<em><b>Address</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Address</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Address</em>' attribute.
	 * @see #setAddress(String)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Address()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='address' namespace='##targetNamespace'"
	 * @generated
	 */
	String getAddress();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getAddress <em>Address</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Address</em>' attribute.
	 * @see #getAddress()
	 * @generated
	 */
	void setAddress(String value);

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
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Name()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Remote Sync</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Remote Sync</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Remote Sync</em>' attribute.
	 * @see #isSetRemoteSync()
	 * @see #unsetRemoteSync()
	 * @see #setRemoteSync(boolean)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_RemoteSync()
	 * @model unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='remoteSync' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isRemoteSync();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#isRemoteSync <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Remote Sync</em>' attribute.
	 * @see #isSetRemoteSync()
	 * @see #unsetRemoteSync()
	 * @see #isRemoteSync()
	 * @generated
	 */
	void setRemoteSync(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.GlobalEndpointType#isRemoteSync <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetRemoteSync()
	 * @see #isRemoteSync()
	 * @see #setRemoteSync(boolean)
	 * @generated
	 */
	void unsetRemoteSync();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.GlobalEndpointType#isRemoteSync <em>Remote Sync</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Remote Sync</em>' attribute is set.
	 * @see #unsetRemoteSync()
	 * @see #isRemoteSync()
	 * @see #setRemoteSync(boolean)
	 * @generated
	 */
	boolean isSetRemoteSync();

	/**
	 * Returns the value of the '<em><b>Remote Sync Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Remote Sync Timeout</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Remote Sync Timeout</em>' attribute.
	 * @see #setRemoteSyncTimeout(String)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_RemoteSyncTimeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='remoteSyncTimeout' namespace='##targetNamespace'"
	 * @generated
	 */
	String getRemoteSyncTimeout();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Remote Sync Timeout</em>' attribute.
	 * @see #getRemoteSyncTimeout()
	 * @generated
	 */
	void setRemoteSyncTimeout(String value);

	/**
	 * Returns the value of the '<em><b>Response Transformers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Response Transformers</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Response Transformers</em>' attribute.
	 * @see #setResponseTransformers(String)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_ResponseTransformers()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='responseTransformers' namespace='##targetNamespace'"
	 * @generated
	 */
	String getResponseTransformers();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getResponseTransformers <em>Response Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Response Transformers</em>' attribute.
	 * @see #getResponseTransformers()
	 * @generated
	 */
	void setResponseTransformers(String value);

	/**
	 * Returns the value of the '<em><b>Synchronous</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Synchronous</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Synchronous</em>' attribute.
	 * @see #isSetSynchronous()
	 * @see #unsetSynchronous()
	 * @see #setSynchronous(boolean)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Synchronous()
	 * @model unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='synchronous' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isSynchronous();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#isSynchronous <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Synchronous</em>' attribute.
	 * @see #isSetSynchronous()
	 * @see #unsetSynchronous()
	 * @see #isSynchronous()
	 * @generated
	 */
	void setSynchronous(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.GlobalEndpointType#isSynchronous <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetSynchronous()
	 * @see #isSynchronous()
	 * @see #setSynchronous(boolean)
	 * @generated
	 */
	void unsetSynchronous();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.GlobalEndpointType#isSynchronous <em>Synchronous</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Synchronous</em>' attribute is set.
	 * @see #unsetSynchronous()
	 * @see #isSynchronous()
	 * @see #setSynchronous(boolean)
	 * @generated
	 */
	boolean isSetSynchronous();

	/**
	 * Returns the value of the '<em><b>Transformers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transformers</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transformers</em>' attribute.
	 * @see #setTransformers(String)
	 * @see org.mule.schema.MulePackage#getGlobalEndpointType_Transformers()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='transformers' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTransformers();

	/**
	 * Sets the value of the '{@link org.mule.schema.GlobalEndpointType#getTransformers <em>Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transformers</em>' attribute.
	 * @see #getTransformers()
	 * @generated
	 */
	void setTransformers(String value);

} // GlobalEndpointType
