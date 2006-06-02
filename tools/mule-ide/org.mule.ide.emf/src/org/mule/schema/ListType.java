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
 * A representation of the model object '<em><b>List Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ListType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ListType#getGroup <em>Group</em>}</li>
 *   <li>{@link org.mule.schema.ListType#getEntry <em>Entry</em>}</li>
 *   <li>{@link org.mule.schema.ListType#getFactoryEntry <em>Factory Entry</em>}</li>
 *   <li>{@link org.mule.schema.ListType#getSystemEntry <em>System Entry</em>}</li>
 *   <li>{@link org.mule.schema.ListType#getContainerEntry <em>Container Entry</em>}</li>
 *   <li>{@link org.mule.schema.ListType#getName <em>Name</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getListType()
 * @model extendedMetaData="name='listType' kind='mixed'"
 * @generated
 */
public interface ListType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getListType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Group</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Group</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Group</em>' attribute list.
	 * @see org.mule.schema.MulePackage#getListType_Group()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='group' name='group:1'"
	 * @generated
	 */
	FeatureMap getGroup();

	/**
	 * Returns the value of the '<em><b>Entry</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.EntryType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Entry</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Entry</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getListType_Entry()
	 * @model type="org.mule.schema.EntryType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='entry' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getEntry();

	/**
	 * Returns the value of the '<em><b>Factory Entry</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.FactoryEntryType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Factory Entry</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Factory Entry</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getListType_FactoryEntry()
	 * @model type="org.mule.schema.FactoryEntryType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='factory-entry' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getFactoryEntry();

	/**
	 * Returns the value of the '<em><b>System Entry</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.SystemEntryType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>System Entry</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>System Entry</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getListType_SystemEntry()
	 * @model type="org.mule.schema.SystemEntryType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='system-entry' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getSystemEntry();

	/**
	 * Returns the value of the '<em><b>Container Entry</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.ContainerEntryType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Container Entry</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Container Entry</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getListType_ContainerEntry()
	 * @model type="org.mule.schema.ContainerEntryType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='container-entry' namespace='##targetNamespace' group='group:1'"
	 * @generated
	 */
	EList getContainerEntry();

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
	 * @see org.mule.schema.MulePackage#getListType_Name()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.mule.schema.ListType#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

} // ListType
