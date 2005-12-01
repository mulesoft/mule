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
 * A representation of the model object '<em><b>Mule Environment Properties Type</b></em>'.
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
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getModel <em>Model</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode <em>Recoverable Mode</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getServerUrl <em>Server Url</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getSynchronousEventTimeout <em>Synchronous Event Timeout</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronousReceive <em>Synchronous Receive</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getTransactionTimeout <em>Transaction Timeout</em>}</li>
 *   <li>{@link org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory <em>Working Directory</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_Mixed()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_ThreadingProfile()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_PoolingProfile()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_QueueProfile()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_PersistenceStrategy()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_ConnectionStrategy()
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_ClientMode()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='clientMode'"
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
	 * Returns the value of the '<em><b>Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Model</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model</em>' attribute.
	 * @see #setModel(String)
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_Model()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='model'"
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_RecoverableMode()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='recoverableMode'"
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
	 * Returns the value of the '<em><b>Server Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Server Url</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Url</em>' attribute.
	 * @see #setServerUrl(String)
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_ServerUrl()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='serverUrl'"
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_Synchronous()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='synchronous'"
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_SynchronousEventTimeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.NMTOKEN"
	 *        extendedMetaData="kind='attribute' name='synchronousEventTimeout'"
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
	 * Returns the value of the '<em><b>Synchronous Receive</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Synchronous Receive</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Synchronous Receive</em>' attribute.
	 * @see #isSetSynchronousReceive()
	 * @see #unsetSynchronousReceive()
	 * @see #setSynchronousReceive(boolean)
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_SynchronousReceive()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='synchronousReceive'"
	 * @generated
	 */
	boolean isSynchronousReceive();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronousReceive <em>Synchronous Receive</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Synchronous Receive</em>' attribute.
	 * @see #isSetSynchronousReceive()
	 * @see #unsetSynchronousReceive()
	 * @see #isSynchronousReceive()
	 * @generated
	 */
	void setSynchronousReceive(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronousReceive <em>Synchronous Receive</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetSynchronousReceive()
	 * @see #isSynchronousReceive()
	 * @see #setSynchronousReceive(boolean)
	 * @generated
	 */
	void unsetSynchronousReceive();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronousReceive <em>Synchronous Receive</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Synchronous Receive</em>' attribute is set.
	 * @see #unsetSynchronousReceive()
	 * @see #isSynchronousReceive()
	 * @see #setSynchronousReceive(boolean)
	 * @generated
	 */
	boolean isSetSynchronousReceive();

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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_TransactionTimeout()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.NMTOKEN"
	 *        extendedMetaData="kind='attribute' name='transactionTimeout'"
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
	 * @see org.mule.schema.SchemaPackage#getMuleEnvironmentPropertiesType_WorkingDirectory()
	 * @model default="./.mule" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='workingDirectory'"
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
