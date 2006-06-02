/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Endpoint Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.EndpointType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getTransaction <em>Transaction</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getSecurityFilter <em>Security Filter</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getAddress <em>Address</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getConnector <em>Connector</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getCreateConnector <em>Create Connector</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#isRemoteSync <em>Remote Sync</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getResponseTransformers <em>Response Transformers</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getTransformers <em>Transformers</em>}</li>
 *   <li>{@link org.mule.schema.EndpointType#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getEndpointType()
 * @model extendedMetaData="name='endpointType' kind='mixed'"
 * @generated
 */
public interface EndpointType extends EObject {
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
     * @see org.mule.schema.MulePackage#getEndpointType_Mixed()
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
     * @see org.mule.schema.MulePackage#getEndpointType_Transaction()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='transaction' namespace='##targetNamespace'"
     * @generated
     */
    TransactionType getTransaction();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getTransaction <em>Transaction</em>}' containment reference.
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
     * @see org.mule.schema.MulePackage#getEndpointType_Filter()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='filter' namespace='##targetNamespace'"
     * @generated
     */
    FilterType getFilter();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getFilter <em>Filter</em>}' containment reference.
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
     * @see org.mule.schema.MulePackage#getEndpointType_SecurityFilter()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='security-filter' namespace='##targetNamespace'"
     * @generated
     */
    SecurityFilterType getSecurityFilter();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getSecurityFilter <em>Security Filter</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Security Filter</em>' containment reference.
     * @see #getSecurityFilter()
     * @generated
     */
    void setSecurityFilter(SecurityFilterType value);

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
     * @see org.mule.schema.MulePackage#getEndpointType_Properties()
     * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
     * @generated
     */
    PropertiesType getProperties();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getProperties <em>Properties</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Properties</em>' containment reference.
     * @see #getProperties()
     * @generated
     */
    void setProperties(PropertiesType value);

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
     * @see org.mule.schema.MulePackage#getEndpointType_Address()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     *        extendedMetaData="kind='attribute' name='address' namespace='##targetNamespace'"
     * @generated
     */
    String getAddress();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getAddress <em>Address</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Address</em>' attribute.
     * @see #getAddress()
     * @generated
     */
    void setAddress(String value);

    /**
     * Returns the value of the '<em><b>Connector</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Connector</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Connector</em>' attribute.
     * @see #setConnector(String)
     * @see org.mule.schema.MulePackage#getEndpointType_Connector()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='connector' namespace='##targetNamespace'"
     * @generated
     */
    String getConnector();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getConnector <em>Connector</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Connector</em>' attribute.
     * @see #getConnector()
     * @generated
     */
    void setConnector(String value);

    /**
     * Returns the value of the '<em><b>Create Connector</b></em>' attribute.
     * The default value is <code>"GET_OR_CREATE"</code>.
     * The literals are from the enumeration {@link org.mule.schema.CreateConnectorType}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Create Connector</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Create Connector</em>' attribute.
     * @see org.mule.schema.CreateConnectorType
     * @see #isSetCreateConnector()
     * @see #unsetCreateConnector()
     * @see #setCreateConnector(CreateConnectorType)
     * @see org.mule.schema.MulePackage#getEndpointType_CreateConnector()
     * @model default="GET_OR_CREATE" unique="false" unsettable="true"
     *        extendedMetaData="kind='attribute' name='createConnector' namespace='##targetNamespace'"
     * @generated
     */
    CreateConnectorType getCreateConnector();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getCreateConnector <em>Create Connector</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Create Connector</em>' attribute.
     * @see org.mule.schema.CreateConnectorType
     * @see #isSetCreateConnector()
     * @see #unsetCreateConnector()
     * @see #getCreateConnector()
     * @generated
     */
    void setCreateConnector(CreateConnectorType value);

    /**
     * Unsets the value of the '{@link org.mule.schema.EndpointType#getCreateConnector <em>Create Connector</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #isSetCreateConnector()
     * @see #getCreateConnector()
     * @see #setCreateConnector(CreateConnectorType)
     * @generated
     */
    void unsetCreateConnector();

    /**
     * Returns whether the value of the '{@link org.mule.schema.EndpointType#getCreateConnector <em>Create Connector</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return whether the value of the '<em>Create Connector</em>' attribute is set.
     * @see #unsetCreateConnector()
     * @see #getCreateConnector()
     * @see #setCreateConnector(CreateConnectorType)
     * @generated
     */
    boolean isSetCreateConnector();

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
     * @see org.mule.schema.MulePackage#getEndpointType_Name()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
     * @generated
     */
    String getName();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getName <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Name</em>' attribute.
     * @see #getName()
     * @generated
     */
    void setName(String value);

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
     * @see org.mule.schema.MulePackage#getEndpointType_Ref()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='ref' namespace='##targetNamespace'"
     * @generated
     */
    String getRef();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getRef <em>Ref</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Ref</em>' attribute.
     * @see #getRef()
     * @generated
     */
    void setRef(String value);

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
     * @see org.mule.schema.MulePackage#getEndpointType_RemoteSync()
     * @model unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
     *        extendedMetaData="kind='attribute' name='remoteSync' namespace='##targetNamespace'"
     * @generated
     */
    boolean isRemoteSync();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#isRemoteSync <em>Remote Sync</em>}' attribute.
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
     * Unsets the value of the '{@link org.mule.schema.EndpointType#isRemoteSync <em>Remote Sync</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #isSetRemoteSync()
     * @see #isRemoteSync()
     * @see #setRemoteSync(boolean)
     * @generated
     */
    void unsetRemoteSync();

    /**
     * Returns whether the value of the '{@link org.mule.schema.EndpointType#isRemoteSync <em>Remote Sync</em>}' attribute is set.
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
     * @see org.mule.schema.MulePackage#getEndpointType_RemoteSyncTimeout()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='remoteSyncTimeout' namespace='##targetNamespace'"
     * @generated
     */
    String getRemoteSyncTimeout();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}' attribute.
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
     * @see #setResponseTransformers(List)
     * @see org.mule.schema.MulePackage#getEndpointType_ResponseTransformers()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.IDREFS" many="false"
     *        extendedMetaData="kind='attribute' name='responseTransformers' namespace='##targetNamespace'"
     * @generated
     */
    List getResponseTransformers();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getResponseTransformers <em>Response Transformers</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Response Transformers</em>' attribute.
     * @see #getResponseTransformers()
     * @generated
     */
    void setResponseTransformers(List value);

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
     * @see org.mule.schema.MulePackage#getEndpointType_Synchronous()
     * @model unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
     *        extendedMetaData="kind='attribute' name='synchronous' namespace='##targetNamespace'"
     * @generated
     */
    boolean isSynchronous();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#isSynchronous <em>Synchronous</em>}' attribute.
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
     * Unsets the value of the '{@link org.mule.schema.EndpointType#isSynchronous <em>Synchronous</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #isSetSynchronous()
     * @see #isSynchronous()
     * @see #setSynchronous(boolean)
     * @generated
     */
    void unsetSynchronous();

    /**
     * Returns whether the value of the '{@link org.mule.schema.EndpointType#isSynchronous <em>Synchronous</em>}' attribute is set.
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
     * @see org.mule.schema.MulePackage#getEndpointType_Transformers()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='transformers' namespace='##targetNamespace'"
     * @generated
     */
    String getTransformers();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getTransformers <em>Transformers</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Transformers</em>' attribute.
     * @see #getTransformers()
     * @generated
     */
    void setTransformers(String value);

    /**
     * Returns the value of the '<em><b>Type</b></em>' attribute.
     * The default value is <code>"senderAndReceiver"</code>.
     * The literals are from the enumeration {@link org.mule.schema.TypeType1}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Type</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Type</em>' attribute.
     * @see org.mule.schema.TypeType1
     * @see #isSetType()
     * @see #unsetType()
     * @see #setType(TypeType1)
     * @see org.mule.schema.MulePackage#getEndpointType_Type()
     * @model default="senderAndReceiver" unique="false" unsettable="true"
     *        extendedMetaData="kind='attribute' name='type' namespace='##targetNamespace'"
     * @generated
     */
    TypeType1 getType();

    /**
     * Sets the value of the '{@link org.mule.schema.EndpointType#getType <em>Type</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @param value the new value of the '<em>Type</em>' attribute.
     * @see org.mule.schema.TypeType1
     * @see #isSetType()
     * @see #unsetType()
     * @see #getType()
     * @generated
     */
    void setType(TypeType1 value);

    /**
     * Unsets the value of the '{@link org.mule.schema.EndpointType#getType <em>Type</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #isSetType()
     * @see #getType()
     * @see #setType(TypeType1)
     * @generated
     */
    void unsetType();

    /**
     * Returns whether the value of the '{@link org.mule.schema.EndpointType#getType <em>Type</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @return whether the value of the '<em>Type</em>' attribute is set.
     * @see #unsetType()
     * @see #getType()
     * @see #setType(TypeType1)
     * @generated
     */
    boolean isSetType();

} // EndpointType
