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
 * A representation of the model object '<em><b>Threading Profile Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#isDoThreading <em>Do Threading</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getId <em>Id</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getMaxBufferSize <em>Max Buffer Size</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getMaxThreadsActive <em>Max Threads Active</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getMaxThreadsIdle <em>Max Threads Idle</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getPoolExhaustedAction <em>Pool Exhausted Action</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getThreadTTL <em>Thread TTL</em>}</li>
 *   <li>{@link org.mule.schema.ThreadingProfileType#getThreadWaitTimeout <em>Thread Wait Timeout</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getThreadingProfileType()
 * @model extendedMetaData="name='threading-profileType' kind='mixed'"
 * @generated
 */
public interface ThreadingProfileType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Do Threading</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Do Threading</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Do Threading</em>' attribute.
	 * @see #isSetDoThreading()
	 * @see #unsetDoThreading()
	 * @see #setDoThreading(boolean)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_DoThreading()
	 * @model default="true" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='doThreading' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isDoThreading();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#isDoThreading <em>Do Threading</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Do Threading</em>' attribute.
	 * @see #isSetDoThreading()
	 * @see #unsetDoThreading()
	 * @see #isDoThreading()
	 * @generated
	 */
	void setDoThreading(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.ThreadingProfileType#isDoThreading <em>Do Threading</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetDoThreading()
	 * @see #isDoThreading()
	 * @see #setDoThreading(boolean)
	 * @generated
	 */
	void unsetDoThreading();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.ThreadingProfileType#isDoThreading <em>Do Threading</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Do Threading</em>' attribute is set.
	 * @see #unsetDoThreading()
	 * @see #isDoThreading()
	 * @see #setDoThreading(boolean)
	 * @generated
	 */
	boolean isSetDoThreading();

	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * The default value is <code>"default"</code>.
	 * The literals are from the enumeration {@link org.mule.schema.IdType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Id</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see org.mule.schema.IdType
	 * @see #isSetId()
	 * @see #unsetId()
	 * @see #setId(IdType)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_Id()
	 * @model default="default" unique="false" unsettable="true"
	 *        extendedMetaData="kind='attribute' name='id' namespace='##targetNamespace'"
	 * @generated
	 */
	IdType getId();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see org.mule.schema.IdType
	 * @see #isSetId()
	 * @see #unsetId()
	 * @see #getId()
	 * @generated
	 */
	void setId(IdType value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.ThreadingProfileType#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetId()
	 * @see #getId()
	 * @see #setId(IdType)
	 * @generated
	 */
	void unsetId();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.ThreadingProfileType#getId <em>Id</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Id</em>' attribute is set.
	 * @see #unsetId()
	 * @see #getId()
	 * @see #setId(IdType)
	 * @generated
	 */
	boolean isSetId();

	/**
	 * Returns the value of the '<em><b>Max Buffer Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Buffer Size</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Buffer Size</em>' attribute.
	 * @see #setMaxBufferSize(String)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_MaxBufferSize()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxBufferSize' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxBufferSize();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getMaxBufferSize <em>Max Buffer Size</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Buffer Size</em>' attribute.
	 * @see #getMaxBufferSize()
	 * @generated
	 */
	void setMaxBufferSize(String value);

	/**
	 * Returns the value of the '<em><b>Max Threads Active</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Threads Active</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Threads Active</em>' attribute.
	 * @see #setMaxThreadsActive(String)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_MaxThreadsActive()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxThreadsActive' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxThreadsActive();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getMaxThreadsActive <em>Max Threads Active</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Threads Active</em>' attribute.
	 * @see #getMaxThreadsActive()
	 * @generated
	 */
	void setMaxThreadsActive(String value);

	/**
	 * Returns the value of the '<em><b>Max Threads Idle</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Threads Idle</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Threads Idle</em>' attribute.
	 * @see #setMaxThreadsIdle(String)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_MaxThreadsIdle()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxThreadsIdle' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxThreadsIdle();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getMaxThreadsIdle <em>Max Threads Idle</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Threads Idle</em>' attribute.
	 * @see #getMaxThreadsIdle()
	 * @generated
	 */
	void setMaxThreadsIdle(String value);

	/**
	 * Returns the value of the '<em><b>Pool Exhausted Action</b></em>' attribute.
	 * The default value is <code>"RUN"</code>.
	 * The literals are from the enumeration {@link org.mule.schema.PoolExhaustedActionType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Pool Exhausted Action</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Pool Exhausted Action</em>' attribute.
	 * @see org.mule.schema.PoolExhaustedActionType
	 * @see #isSetPoolExhaustedAction()
	 * @see #unsetPoolExhaustedAction()
	 * @see #setPoolExhaustedAction(PoolExhaustedActionType)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_PoolExhaustedAction()
	 * @model default="RUN" unique="false" unsettable="true"
	 *        extendedMetaData="kind='attribute' name='poolExhaustedAction' namespace='##targetNamespace'"
	 * @generated
	 */
	PoolExhaustedActionType getPoolExhaustedAction();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getPoolExhaustedAction <em>Pool Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Pool Exhausted Action</em>' attribute.
	 * @see org.mule.schema.PoolExhaustedActionType
	 * @see #isSetPoolExhaustedAction()
	 * @see #unsetPoolExhaustedAction()
	 * @see #getPoolExhaustedAction()
	 * @generated
	 */
	void setPoolExhaustedAction(PoolExhaustedActionType value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.ThreadingProfileType#getPoolExhaustedAction <em>Pool Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetPoolExhaustedAction()
	 * @see #getPoolExhaustedAction()
	 * @see #setPoolExhaustedAction(PoolExhaustedActionType)
	 * @generated
	 */
	void unsetPoolExhaustedAction();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.ThreadingProfileType#getPoolExhaustedAction <em>Pool Exhausted Action</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Pool Exhausted Action</em>' attribute is set.
	 * @see #unsetPoolExhaustedAction()
	 * @see #getPoolExhaustedAction()
	 * @see #setPoolExhaustedAction(PoolExhaustedActionType)
	 * @generated
	 */
	boolean isSetPoolExhaustedAction();

	/**
	 * Returns the value of the '<em><b>Thread TTL</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Thread TTL</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Thread TTL</em>' attribute.
	 * @see #setThreadTTL(String)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_ThreadTTL()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='threadTTL' namespace='##targetNamespace'"
	 * @generated
	 */
	String getThreadTTL();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getThreadTTL <em>Thread TTL</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Thread TTL</em>' attribute.
	 * @see #getThreadTTL()
	 * @generated
	 */
	void setThreadTTL(String value);

	/**
	 * Returns the value of the '<em><b>Thread Wait Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Thread Wait Timeout</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Thread Wait Timeout</em>' attribute.
	 * @see #setThreadWaitTimeout(String)
	 * @see org.mule.schema.MulePackage#getThreadingProfileType_ThreadWaitTimeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='threadWaitTimeout' namespace='##targetNamespace'"
	 * @generated
	 */
	String getThreadWaitTimeout();

	/**
	 * Sets the value of the '{@link org.mule.schema.ThreadingProfileType#getThreadWaitTimeout <em>Thread Wait Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Thread Wait Timeout</em>' attribute.
	 * @see #getThreadWaitTimeout()
	 * @generated
	 */
	void setThreadWaitTimeout(String value);

} // ThreadingProfileType
