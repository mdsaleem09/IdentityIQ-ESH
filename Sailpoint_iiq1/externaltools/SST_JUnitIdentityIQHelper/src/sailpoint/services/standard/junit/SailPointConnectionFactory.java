package sailpoint.services.standard.junit;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import sailpoint.api.SailPointContext;

/**
 * @author christian.cairney
 *
 */
public class SailPointConnectionFactory {

	private Logger log;
	private SailPointExternalContext ctx = null;
	private String username = null;
	private String password = null;
	
	private static SailPointConnectionFactory spcf = new SailPointConnectionFactory();
	
	/**
	 * Do not allow the creation of this class, force to be 
	 * a singleton.
	 */
	private SailPointConnectionFactory() {
		
	}
	
	/**
	 * This factory class is a singleton, so return the 
	 * instance instantiated at runtime.
	 * 
	 * @return
	 */
	public static final SailPointConnectionFactory getInstance() {
		return SailPointConnectionFactory.spcf;
	}
	
	/**
	 * Get the default username, used when instantiating IdentityIQ
	 * 
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the default username for IdentityIQ
	 * 
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the default password for IdentityIQ
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the current IdentityIQ SP Context, or spin up a new
	 * instance of IdentityIQ if one is not available
	 * 
	 * @return
	 * @throws Exception
	 */
	public SailPointContext getContext() throws Exception {
		return getContext(this.username, this.password);
	}
	
	/**
	 * Get the current context or create a new context if one isn't available
	 * 
	 * Setting a different username will to an existing session will trigger the session
	 * to be closed down, and then a new one will be instantiated with the new
	 * username and password supplied in the signature.
	 * 
	 * @param username
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public SailPointContext getContext(String username, String password) throws Exception {
		
		if (username != null) this.setUsername(username);
		if (password != null) this.setPassword(password);
		
		// If we are changing the username, then close the instance and 
		// log in as another user.
		
		if (!username.equals(this.username)) {
			if (ctx != null) ctx.close();
		}
		
		// If we already have a context, then don't get it again :)
		if (ctx != null) return ctx.getSailPointContext();
		
		ctx = new SailPointExternalContext(this.username, this.password);
		ctx.start();
		
		return ctx.getSailPointContext();
		
	}
	
	/**
	 * Close the current context and shutdown IdentityIQ's local instance
	 * 
	 */
	public void closeContext() {
		
		if (ctx != null) ctx.close();
		ctx = null;
		
	}
}
