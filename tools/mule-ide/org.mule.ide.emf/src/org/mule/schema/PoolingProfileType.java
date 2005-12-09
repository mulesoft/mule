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
 * A representation of the model object '<em><b>Pooling Profile Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.PoolingProfileType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.PoolingProfileType#getExhaustedAction <em>Exhausted Action</em>}</li>
 *   <li>{@link org.mule.schema.PoolingProfileType#getFactory <em>Factory</em>}</li>
 *   <li>{@link org.mule.schema.PoolingProfileType#getInitialisationPolicy <em>Initialisation Policy</em>}</li>
 *   <li>{@link org.mule.schema.PoolingProfileType#getMaxActive <em>Max Active</em>}</li>
 *   <li>{@link org.mule.schema.PoolingProfileType#getMaxIdle <em>Max Idle</em>}</li>
 *   <li>{@link org.mule.schema.PoolingProfileType#getMaxWait <em>Max Wait</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getPoolingProfileType()
 * @model extendedMetaData="name='pooling-profileType' kind='mixed'"
 * @generated
 */
public interface PoolingProfileType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Exhausted Action</b></em>' attribute.
	 * The default value is <code>"GROW"</code>.
	 * The literals are from the enumeration {@link org.mule.schema.ExhaustedActionType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exhausted Action</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exhausted Action</em>' attribute.
	 * @see org.mule.schema.ExhaustedActionType
	 * @see #isSetExhaustedAction()
	 * @see #unsetExhaustedAction()
	 * @see #setExhaustedAction(ExhaustedActionType)
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_ExhaustedAction()
	 * @model default="GROW" unique="false" unsettable="true"
	 *        extendedMetaData="kind='attribute' name='exhaustedAction' namespace='##targetNamespace'"
	 * @generated
	 */
	ExhaustedActionType getExhaustedAction();

	/**
	 * Sets the value of the '{@link org.mule.schema.PoolingProfileType#getExhaustedAction <em>Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exhausted Action</em>' attribute.
	 * @see org.mule.schema.ExhaustedActionType
	 * @see #isSetExhaustedAction()
	 * @see #unsetExhaustedAction()
	 * @see #getExhaustedAction()
	 * @generated
	 */
	void setExhaustedAction(ExhaustedActionType value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.PoolingProfileType#getExhaustedAction <em>Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetExhaustedAction()
	 * @see #getExhaustedAction()
	 * @see #setExhaustedAction(ExhaustedActionType)
	 * @generated
	 */
	void unsetExhaustedAction();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.PoolingProfileType#getExhaustedAction <em>Exhausted Action</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Exhausted Action</em>' attribute is set.
	 * @see #unsetExhaustedAction()
	 * @see #getExhaustedAction()
	 * @see #setExhaustedAction(ExhaustedActionType)
	 * @generated
	 */
	boolean isSetExhaustedAction();

	/**
	 * Returns the value of the '<em><b>Factory</b></em>' attribute.
	 * The default value is <code>"org.mule.config.pool.CommonsPoolFactory"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Factory</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Factory</em>' attribute.
	 * @see #isSetFactory()
	 * @see #unsetFactory()
	 * @see #setFactory(String)
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_Factory()
	 * @model default="org.mule.config.pool.CommonsPoolFactory" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='factory' namespace='##targetNamespace'"
	 * @generated
	 */
	String getFactory();

	/**
	 * Sets the value of the '{@link org.mule.schema.PoolingProfileType#getFactory <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Factory</em>' attribute.
	 * @see #isSetFactory()
	 * @see #unsetFactory()
	 * @see #getFactory()
	 * @generated
	 */
	void setFactory(String value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.PoolingProfileType#getFactory <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetFactory()
	 * @see #getFactory()
	 * @see #setFactory(String)
	 * @generated
	 */
	void unsetFactory();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.PoolingProfileType#getFactory <em>Factory</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Factory</em>' attribute is set.
	 * @see #unsetFactory()
	 * @see #getFactory()
	 * @see #setFactory(String)
	 * @generated
	 */
	boolean isSetFactory();

	/**
	 * Returns the value of the '<em><b>Initialisation Policy</b></em>' attribute.
	 * The default value is <code>"INITIALISE_FIRST"</code>.
	 * The literals are from the enumeration {@link org.mule.schema.InitialisationPolicyType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Initialisation Policy</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Initialisation Policy</em>' attribute.
	 * @see org.mule.schema.InitialisationPolicyType
	 * @see #isSetInitialisationPolicy()
	 * @see #unsetInitialisationPolicy()
	 * @see #setInitialisationPolicy(InitialisationPolicyType)
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_InitialisationPolicy()
	 * @model default="INITIALISE_FIRST" unique="false" unsettable="true"
	 *        extendedMetaData="kind='attribute' name='initialisationPolicy' namespace='##targetNamespace'"
	 * @generated
	 */
	InitialisationPolicyType getInitialisationPolicy();

	/**
	 * Sets the value of the '{@link org.mule.schema.PoolingProfileType#getInitialisationPolicy <em>Initialisation Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Initialisation Policy</em>' attribute.
	 * @see org.mule.schema.InitialisationPolicyType
	 * @see #isSetInitialisationPolicy()
	 * @see #unsetInitialisationPolicy()
	 * @see #getInitialisationPolicy()
	 * @generated
	 */
	void setInitialisationPolicy(InitialisationPolicyType value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.PoolingProfileType#getInitialisationPolicy <em>Initialisation Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetInitialisationPolicy()
	 * @see #getInitialisationPolicy()
	 * @see #setInitialisationPolicy(InitialisationPolicyType)
	 * @generated
	 */
	void unsetInitialisationPolicy();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.PoolingProfileType#getInitialisationPolicy <em>Initialisation Policy</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Initialisation Policy</em>' attribute is set.
	 * @see #unsetInitialisationPolicy()
	 * @see #getInitialisationPolicy()
	 * @see #setInitialisationPolicy(InitialisationPolicyType)
	 * @generated
	 */
	boolean isSetInitialisationPolicy();

	/**
	 * Returns the value of the '<em><b>Max Active</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Active</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Active</em>' attribute.
	 * @see #setMaxActive(String)
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_MaxActive()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxActive' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxActive();

	/**
	 * Sets the value of the '{@link org.mule.schema.PoolingProfileType#getMaxActive <em>Max Active</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Active</em>' attribute.
	 * @see #getMaxActive()
	 * @generated
	 */
	void setMaxActive(String value);

	/**
	 * Returns the value of the '<em><b>Max Idle</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Idle</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Idle</em>' attribute.
	 * @see #setMaxIdle(String)
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_MaxIdle()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxIdle' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxIdle();

	/**
	 * Sets the value of the '{@link org.mule.schema.PoolingProfileType#getMaxIdle <em>Max Idle</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Idle</em>' attribute.
	 * @see #getMaxIdle()
	 * @generated
	 */
	void setMaxIdle(String value);

	/**
	 * Returns the value of the '<em><b>Max Wait</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Max Wait</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Wait</em>' attribute.
	 * @see #setMaxWait(String)
	 * @see org.mule.schema.MulePackage#getPoolingProfileType_MaxWait()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='maxWait' namespace='##targetNamespace'"
	 * @generated
	 */
	String getMaxWait();

	/**
	 * Sets the value of the '{@link org.mule.schema.PoolingProfileType#getMaxWait <em>Max Wait</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Wait</em>' attribute.
	 * @see #getMaxWait()
	 * @generated
	 */
	void setMaxWait(String value);

} // PoolingProfileType
