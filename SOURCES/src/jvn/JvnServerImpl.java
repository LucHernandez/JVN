/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact:
 *
 * Authors:
 */

package jvn;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;


public class JvnServerImpl
		extends UnicastRemoteObject
		implements JvnLocalServer, JvnRemoteServer {

	/**
	 *
	 */
	private static final long serialVersionUID = new Random().nextLong();;;
	// A JVN server is managed as a singleton
	private static JvnServerImpl js = null;
    private final JvnRemoteCoord coordinator;
	public UUID id;
	private JvnObject jo;
	private HashMap<UUID, JvnObject> objs;

	/**
	 * Default constructor
	 *
	 * @throws JvnException
	 **/
	private JvnServerImpl(String port) throws Exception {
		super();
        Registry registry = LocateRegistry.getRegistry();
		System.out.println("FOUND REGISTRY");
		registry.rebind("server"+serialVersionUID, this);
		this.coordinator = (JvnRemoteCoord) registry.lookup("rmi://localhost/coordinateur");
		this.objs = new HashMap<UUID, JvnObject>();
		if (this.coordinator == null) {
			System.out.println("DID NOT FIND COORDINATOR");
		} else {
			System.out.println("FOUND COORDINATOR");
		}
		id = this.coordinator.jvnGetObjectId();
	}

	/**
	 * Static method allowing an application to get a reference to
	 * a JVN server instance
	 *
	 * @throws JvnException
	 **/
	public static JvnServerImpl jvnGetServer() throws JvnException{
		if (js == null) {
			try {
				js = new JvnServerImpl("1099");
			} catch (Exception e) {
				throw new JvnException("No rmi server or port available" + e.getMessage());
			}
		}
		return js;
	}

	/**
	 * The JVN service is not used anymore
	 *
	 * @throws JvnException
	 **/
	public void jvnTerminate()
			throws jvn.JvnException {
		try {
			this.coordinator.jvnTerminate(js);
			UnicastRemoteObject.unexportObject(this, true);
		}
		catch (RemoteException re){
			throw new JvnException("Erreur de fermeture : " + re.getMessage());
		}
	}

	/**
	 * creation of a JVN object
	 *
	 * @param o : the JVN object state
	 * @throws JvnException
	 **/
	public JvnObject jvnCreateObject(Serializable o)
			throws jvn.JvnException {
		System.out.println("JvnServerImpl.jvnCreateObject");
		//	Créer l'objet
		// Register l'objet vers le coordinateur
		try  {
			JvnObject objet = new JvnObjectImpl(this, o, coordinator.jvnGetObjectId());
			System.out.println("Object created" + objet);
			coordinator.jvnRegisterObject(objet.jvnGetObjectId().toString(), objet, this);
			System.out.println("Object registered");
			this.objs.put(this.coordinator.jvnGetObjectId(), objet);
			this.jvnLockWrite(objet.jvnGetObjectId());
			return objet;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new JvnException();
		}
	}

	/**
	 * Associate a symbolic name with a JVN object
	 *
	 * @param jon : the JVN object name
	 * @param jo  : the JVN object
	 * @throws JvnException
	 **/
	public void jvnRegisterObject(String jon, JvnObject jo)
			throws jvn.JvnException {
		try{
			coordinator.jvnRegisterObject(jon, jo, this);
		} catch (RemoteException e) {
            throw new JvnException("Erreur lors de l'enregistrement d'un objet : "+e.getMessage());
        }
    }

	/**
	 * Provide the reference of a JVN object beeing given its symbolic name
	 *
	 * @param jon : the JVN object name
	 * @return the JVN object
	 * @throws JvnException
	 **/
	public JvnObject jvnLookupObject(String jon)
			throws jvn.JvnException, NotBoundException, RemoteException {
		try {
			return coordinator.jvnLookupObject(jon, this);
		} catch (JvnException e) {
            throw new JvnException("Erreur lors de la recupération de l'objet : " + e.getMessage());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
		catch (Exception e){
			return null;
		}
	}

	/**
	 * Get a Read lock on a JVN object
	 *
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws JvnException
	 **/
	public Serializable jvnLockRead(UUID joi) throws JvnException {
		try {
			return coordinator.jvnLockRead(joi, this);
		} catch (Exception e) {
			throw new JvnException("Erreur lors de l'acquisition du verrou : " + e.getMessage());
		}
	}


	/**
	 * Get a Write lock on a JVN object
	 *
	 * @param joi : the JVN object identification
	 * @return the current JVN object state
	 * @throws JvnException
	 **/
	public Serializable jvnLockWrite(UUID joi)
			throws JvnException {
		try {
			return coordinator.jvnLockWrite(joi, this);
		} catch (Exception e) {
			throw new JvnException("Erreur lors de l'acquisition du verrou : " + e.getMessage());
		}
	}


	/**
	 * Invalidate the Read lock of the JVN object identified by id
	 * called by the JvnCoord
	 *
	 * @param joi : the JVN object id
	 * @return void
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public void jvnInvalidateReader(UUID joi)
			throws java.rmi.RemoteException, jvn.JvnException {
		try{
			jo.jvnInvalidateReader();
		} catch (JvnException e) {
			throw new RemoteException("Remote Exception R");
		}
	}

	/**
	 * Invalidate the Write lock of the JVN object identified by id
	 *
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriter(UUID joi)
			throws java.rmi.RemoteException, jvn.JvnException {
		try{
			return jo.jvnInvalidateWriter();
		} catch (JvnException e) {
			throw new RemoteException("Remote Exception W");
		}
	}

	/**
	 * Reduce the Write lock of the JVN object identified by id
	 *
	 * @param joi : the JVN object id
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public Serializable jvnInvalidateWriterForReader(UUID joi)
			throws java.rmi.RemoteException, jvn.JvnException {
		try{
			return jo.jvnInvalidateWriterForReader();
		} catch (JvnException e) {
            throw new RemoteException("Remote Exception W -> R");
        }
	}
}


