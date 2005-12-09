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
 * A representation of the model object '<em><b>Environment Properties Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getThreadingProfile <em>Threading Profile</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getPoolingProfile <em>Pooling Profile</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getQueueProfile <em>Queue Profile</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getPersistenceStrategy <em>Persistence Strategy</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getConnectionStrategy <em>Connection Strategy</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isClientMode <em>Client Mode</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isEmbedded <em>Embedded</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isEnableMessageEvents <em>Enable Message Events</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getEncoding <em>Encoding</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getModel <em>Model</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode <em>Recoverable Mode</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isRemoteSync <em>Remote Sync</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getServerUrl <em>Server Url</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getSynchronousEventTimeout <em>Synchronous Event Timeout</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getTransactionTimeout <em>Transaction Timeout</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory <em>Working Directory</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType()
 * @model extendedMetaData="name='mule-environment-propertiesType' kind='mixed'"
 * @generated
 */
public interface MuleEnvironmentPropertiesType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Threading Profile</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.ThreadingProfileType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Threading Profile</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Threading Profile</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_ThreadingProfile()
	 * @model type="org.mule.schema.ThreadingProfileType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='threading-profile' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getThreadingProfile();

	/**
	 * Returns the value of the '<em><b>Pooling Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Pooling Profile</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Pooling Profile</em>' containment reference.
	 * @see #setPoolingProfile(PoolingProfileType)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_PoolingProfile()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='pooling-profile' namespace='##targetNamespace'"
	 * @generated
	 */
	PoolingProfileType getPoolingProfile();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getPoolingProfile <em>Pooling Profile</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Pooling Profile</em>' containment reference.
	 * @see #getPoolingProfile()
	 * @generated
	 */
	void setPoolingProfile(PoolingProfileType value);

	/**
	 * Returns the value of the '<em><b>Queue Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Queue Profile</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Queue Profile</em>' containment reference.
	 * @see #setQueueProfile(QueueProfileType)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_QueueProfile()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='queue-profile' namespace='##targetNamespace'"
	 * @generated
	 */
	QueueProfileType getQueueProfile();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getQueueProfile <em>Queue Profile</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Queue Profile</em>' containment reference.
	 * @see #getQueueProfile()
	 * @generated
	 */
	void setQueueProfile(QueueProfileType value);

	/**
	 * Returns the value of the '<em><b>Persistence Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Persistence Strategy</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Persistence Strategy</em>' containment reference.
	 * @see #setPersistenceStrategy(PersistenceStrategyType)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_PersistenceStrategy()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='persistence-strategy' namespace='##targetNamespace'"
	 * @generated
	 */
	PersistenceStrategyType getPersistenceStrategy();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getPersistenceStrategy <em>Persistence Strategy</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Persistence Strategy</em>' containment reference.
	 * @see #getPersistenceStrategy()
	 * @generated
	 */
	void setPersistenceStrategy(PersistenceStrategyType value);

	/**
	 * Returns the value of the '<em><b>Connection Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Connection Strategy</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Connection Strategy</em>' containment reference.
	 * @see #setConnectionStrategy(ConnectionStrategyType)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_ConnectionStrategy()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='connection-strategy' namespace='##targetNamespace'"
	 * @generated
	 */
	ConnectionStrategyType getConnectionStrategy();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getConnectionStrategy <em>Connection Strategy</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Connection Strategy</em>' containment reference.
	 * @see #getConnectionStrategy()
	 * @generated
	 */
	void setConnectionStrategy(ConnectionStrategyType value);

	/**
	 * Returns the value of the '<em><b>Client Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Client Mode</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Client Mode</em>' attribute.
	 * @see #isSetClientMode()
	 * @see #unsetClientMode()
	 * @see #setClientMode(boolean)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_ClientMode()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='clientMode' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isClientMode();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isClientMode <em>Client Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Client Mode</em>' attribute.
	 * @see #isSetClientMode()
	 * @see #unsetClientMode()
	 * @see #isClientMode()
	 * @generated
	 */
	void setClientMode(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isClientMode <em>Client Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetClientMode()
	 * @see #isClientMode()
	 * @see #setClientMode(boolean)
	 * @generated
	 */
	void unsetClientMode();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isClientMode <em>Client Mode</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Client Mode</em>' attribute is set.
	 * @see #unsetClientMode()
	 * @see #isClientMode()
	 * @see #setClientMode(boolean)
	 * @generated
	 */
	boolean isSetClientMode();

	/**
	 * Returns the value of the '<em><b>Embedded</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Embedded</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Embedded</em>' attribute.
	 * @see #isSetEmbedded()
	 * @see #unsetEmbedded()
	 * @see #setEmbedded(boolean)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_Embedded()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='embedded' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isEmbedded();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEmbedded <em>Embedded</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Embedded</em>' attribute.
	 * @see #isSetEmbedded()
	 * @see #unsetEmbedded()
	 * @see #isEmbedded()
	 * @generated
	 */
	void setEmbedded(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEmbedded <em>Embedded</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetEmbedded()
	 * @see #isEmbedded()
	 * @see #setEmbedded(boolean)
	 * @generated
	 */
	void unsetEmbedded();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEmbedded <em>Embedded</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Embedded</em>' attribute is set.
	 * @see #unsetEmbedded()
	 * @see #isEmbedded()
	 * @see #setEmbedded(boolean)
	 * @generated
	 */
	boolean isSetEmbedded();

	/**
	 * Returns the value of the '<em><b>Enable Message Events</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Enable Message Events</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Enable Message Events</em>' attribute.
	 * @see #isSetEnableMessageEvents()
	 * @see #unsetEnableMessageEvents()
	 * @see #setEnableMessageEvents(boolean)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_EnableMessageEvents()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='enableMessageEvents' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isEnableMessageEvents();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEnableMessageEvents <em>Enable Message Events</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Enable Message Events</em>' attribute.
	 * @see #isSetEnableMessageEvents()
	 * @see #unsetEnableMessageEvents()
	 * @see #isEnableMessageEvents()
	 * @generated
	 */
	void setEnableMessageEvents(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEnableMessageEvents <em>Enable Message Events</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetEnableMessageEvents()
	 * @see #isEnableMessageEvents()
	 * @see #setEnableMessageEvents(boolean)
	 * @generated
	 */
	void unsetEnableMessageEvents();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEnableMessageEvents <em>Enable Message Events</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Enable Message Events</em>' attribute is set.
	 * @see #unsetEnableMessageEvents()
	 * @see #isEnableMessageEvents()
	 * @see #setEnableMessageEvents(boolean)
	 * @generated
	 */
	boolean isSetEnableMessageEvents();

	/**
	 * Returns the value of the '<em><b>Encoding</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Encoding</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Encoding</em>' attribute.
	 * @see #setEncoding(String)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_Encoding()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='encoding' namespace='##targetNamespace'"
	 * @generated
	 */
	String getEncoding();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getEncoding <em>Encoding</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Encoding</em>' attribute.
	 * @see #getEncoding()
	 * @generated
	 */
	void setEncoding(String value);

	/**
	 * Returns the value of the '<em><b>Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Model</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model</em>' attribute.
	 * @see #setModel(String)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_Model()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='model' namespace='##targetNamespace'"
	 * @generated
	 */
	String getModel();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getModel <em>Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Model</em>' attribute.
	 * @see #getModel()
	 * @generated
	 */
	void setModel(String value);

	/**
	 * Returns the value of the '<em><b>Recoverable Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Recoverable Mode</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Recoverable Mode</em>' attribute.
	 * @see #isSetRecoverableMode()
	 * @see #unsetRecoverableMode()
	 * @see #setRecoverableMode(boolean)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_RecoverableMode()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='recoverableMode' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isRecoverableMode();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode <em>Recoverable Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Recoverable Mode</em>' attribute.
	 * @see #isSetRecoverableMode()
	 * @see #unsetRecoverableMode()
	 * @see #isRecoverableMode()
	 * @generated
	 */
	void setRecoverableMode(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode <em>Recoverable Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetRecoverableMode()
	 * @see #isRecoverableMode()
	 * @see #setRecoverableMode(boolean)
	 * @generated
	 */
	void unsetRecoverableMode();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode <em>Recoverable Mode</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Recoverable Mode</em>' attribute is set.
	 * @see #unsetRecoverableMode()
	 * @see #isRecoverableMode()
	 * @see #setRecoverableMode(boolean)
	 * @generated
	 */
	boolean isSetRecoverableMode();

	/**
	 * Returns the value of the '<em><b>Remote Sync</b></em>' attribute.
	 * The default value is <code>"false"</code>.
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
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_RemoteSync()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='remoteSync' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isRemoteSync();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRemoteSync <em>Remote Sync</em>}' attribute.
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
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRemoteSync <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetRemoteSync()
	 * @see #isRemoteSync()
	 * @see #setRemoteSync(boolean)
	 * @generated
	 */
	void unsetRemoteSync();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRemoteSync <em>Remote Sync</em>}' attribute is set.
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
	 * Returns the value of the '<em><b>Server Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Server Url</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Url</em>' attribute.
	 * @see #setServerUrl(String)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_ServerUrl()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='serverUrl' namespace='##targetNamespace'"
	 * @generated
	 */
	String getServerUrl();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getServerUrl <em>Server Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Server Url</em>' attribute.
	 * @see #getServerUrl()
	 * @generated
	 */
	void setServerUrl(String value);

	/**
	 * Returns the value of the '<em><b>Synchronous</b></em>' attribute.
	 * The default value is <code>"false"</code>.
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
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_Synchronous()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='synchronous' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isSynchronous();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous <em>Synchronous</em>}' attribute.
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
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetSynchronous()
	 * @see #isSynchronous()
	 * @see #setSynchronous(boolean)
	 * @generated
	 */
	void unsetSynchronous();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous <em>Synchronous</em>}' attribute is set.
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
	 * Returns the value of the '<em><b>Synchronous Event Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Synchronous Event Timeout</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Synchronous Event Timeout</em>' attribute.
	 * @see #setSynchronousEventTimeout(String)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_SynchronousEventTimeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='synchronousEventTimeout' namespace='##targetNamespace'"
	 * @generated
	 */
	String getSynchronousEventTimeout();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getSynchronousEventTimeout <em>Synchronous Event Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Synchronous Event Timeout</em>' attribute.
	 * @see #getSynchronousEventTimeout()
	 * @generated
	 */
	void setSynchronousEventTimeout(String value);

	/**
	 * Returns the value of the '<em><b>Transaction Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transaction Timeout</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transaction Timeout</em>' attribute.
	 * @see #setTransactionTimeout(String)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_TransactionTimeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='transactionTimeout' namespace='##targetNamespace'"
	 * @generated
	 */
	String getTransactionTimeout();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getTransactionTimeout <em>Transaction Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transaction Timeout</em>' attribute.
	 * @see #getTransactionTimeout()
	 * @generated
	 */
	void setTransactionTimeout(String value);

	/**
	 * Returns the value of the '<em><b>Working Directory</b></em>' attribute.
	 * The default value is <code>"./.mule"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Working Directory</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Working Directory</em>' attribute.
	 * @see #isSetWorkingDirectory()
	 * @see #unsetWorkingDirectory()
	 * @see #setWorkingDirectory(String)
	 * @see org.mule.schema.MulePackage#getMuleEnvironmentPropertiesType_WorkingDirectory()
	 * @model default="./.mule" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='workingDirectory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getWorkingDirectory();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory <em>Working Directory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Working Directory</em>' attribute.
	 * @see #isSetWorkingDirectory()
	 * @see #unsetWorkingDirectory()
	 * @see #getWorkingDirectory()
	 * @generated
	 */
	void setWorkingDirectory(String value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory <em>Working Directory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetWorkingDirectory()
	 * @see #getWorkingDirectory()
	 * @see #setWorkingDirectory(String)
	 * @generated
	 */
	void unsetWorkingDirectory();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory <em>Working Directory</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Working Directory</em>' attribute is set.
	 * @see #unsetWorkingDirectory()
	 * @see #getWorkingDirectory()
	 * @see #setWorkingDirectory(String)
	 * @generated
	 */
	boolean isSetWorkingDirectory();

} // MuleEnvironmentPropertiesType
