/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.*;


public class JvnCoordImpl
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{

    HashMap<UUID, List<JvnRemoteServer>> readList;
    HashMap<UUID, JvnRemoteServer> writeList;
    HashMap<String, JvnObject> objects;

    public static void main() {
        try {
            JvnCoordImpl jvnServer = new JvnCoordImpl();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws RemoteException {
		// to be completed
        super();
        Registry registry = null;
        readList = new HashMap<UUID, List<JvnRemoteServer>>();
        writeList = new HashMap<UUID, JvnRemoteServer>();
        objects = new HashMap<String, JvnObject>();
        registry = LocateRegistry.createRegistry(1099);
        System.out.println("COORDINATEUR UP !");

        try {
            registry.rebind("rmi://localhost/coordinateur", this);
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("COORDINATEUR UP !");
	}

  /**
   * Allocate a NEW JVN object id (usually allocated to a
   * newly created JVN object)
   *
   * @throws java.rmi.RemoteException,JvnException
   **/
  public UUID jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
    return UUID.randomUUID();
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  /* @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
      System.out.println("registering object");
    // to be completed
      if (objects.containsKey(jo.toString())) {
          throw new JvnException("Object already exists in coordinator !");
      }
      objects.put(jo.toString(), jo);
      readList.put(jo.jvnGetObjectId(), new ArrayList<JvnRemoteServer>());
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
      JvnObject objet = objects.get(jon);
      objet.jvnUnLock();
      return objet;
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(UUID joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
       JvnRemoteServer writer = writeList.get(joi);
       if (writer != null) {
           objects.put(joi.toString(), (JvnObject) writer.jvnInvalidateWriterForReader(joi));
           writer = null;
       }
       return objects.get(joi.toString());
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(UUID joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
       JvnRemoteServer writer = writeList.get(joi);
       if (writer != null) {
           objects.put(joi.toString(), (JvnObject) writer.jvnInvalidateWriterForReader(joi));
           writer = null;
       }
       readList.get(joi).forEach(reader -> {
           try {
               reader.jvnInvalidateReader(joi);
           } catch (Exception e) {

           }
       });
    return objects.get(joi.toString());
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
	 writeList.forEach((key, value) -> {
         try {
             if (value.equals(js)) {
                 writeList.remove(key);
             }
         } catch (Exception e) {
             throw new RuntimeException(e);
         }
     });
     readList.forEach((key, list) -> {
         list.forEach(reader -> {
             try {
                 if (reader.equals(js)) {
                     readList.get(key).remove(reader);
                     return;
                 }
             } catch (Exception e) {
                 throw new RuntimeException(e);
             }
         });
         if  (readList.get(key).isEmpty()) {
             readList.remove(key);
         }
     });
    }
}

 
