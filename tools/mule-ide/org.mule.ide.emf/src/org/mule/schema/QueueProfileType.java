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
 * A representation of the model object '<em><b>Queue Profile Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.QueueProfileType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.QueueProfileType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.QueueProfileType#getMaxOutstandingMessages <em>Max Outstanding Messages</em>}</li>
 *   <li>{@link org.mule.schema.QueueProfileType#isPersistent <em>Persistent</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getQueueProfileType()
 * @model extendedMetaData="name='queue-profileType' kind='mixed'"
 * @generated
 */
public interface QueueProfileType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getQueueProfileType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Properties</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference.
	 * @see #setProperties(PropertiesType)
	 * @see org.mule.schema.MulePackage#getQueueProfileType_Properties()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	PropertiesType getProperties();

	/**
	 * Sets the value of the '{@link org.mule.schema.QueueProfileType#getProperties <em>Properties</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Properties</em>' containment reference.
	 * @see #getProperties()
	 * @generated
	 */
	void setProperties(PropertiesType value);

	/**
	 * Returns the value of the '<em><b>Max Outstanding Messages</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Outstanding Messages</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Outstanding Messages</em>' attribute.
	 * @see #setMaxOutstandingMessages(String)
	 * @see org.mule.schema.MulePackage#getQueueProfileType_MaxOutstandingMessages()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxOutstandingMessages' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxOutstandingMessages();

	/**
	 * Sets the value of the '{@link org.mule.schema.QueueProfileType#getMaxOutstandingMessages <em>Max Outstanding Messages</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Outstanding Messages</em>' attribute.
	 * @see #getMaxOutstandingMessages()
	 * @generated
	 */
	void setMaxOutstandingMessages(String value);

	/**
	 * Returns the value of the '<em><b>Persistent</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Persistent</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Persistent</em>' attribute.
	 * @see #isSetPersistent()
	 * @see #unsetPersistent()
	 * @see #setPersistent(boolean)
	 * @see org.mule.schema.MulePackage#getQueueProfileType_Persistent()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='persistent' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isPersistent();

	/**
	 * Sets the value of the '{@link org.mule.schema.QueueProfileType#isPersistent <em>Persistent</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Persistent</em>' attribute.
	 * @see #isSetPersistent()
	 * @see #unsetPersistent()
	 * @see #isPersistent()
	 * @generated
	 */
	void setPersistent(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.QueueProfileType#isPersistent <em>Persistent</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetPersistent()
	 * @see #isPersistent()
	 * @see #setPersistent(boolean)
	 * @generated
	 */
	void unsetPersistent();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.QueueProfileType#isPersistent <em>Persistent</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Persistent</em>' attribute is set.
	 * @see #unsetPersistent()
	 * @see #isPersistent()
	 * @see #setPersistent(boolean)
	 * @generated
	 */
	boolean isSetPersistent();

} // QueueProfileType
