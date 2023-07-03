package sailpoint.services.standard.junit;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import javax.naming.*;
import java.util.Hashtable;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;

import javax.naming.InitialContext;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.connector.ExpiredPasswordException;
import sailpoint.launch.Launcher;
import sailpoint.spring.SpringStarter;
import sailpoint.tools.BrandingServiceFactory;
//import sailpoint.tools.BrandingServiceFactory;
import sailpoint.tools.GeneralException;

public class SailPointExternalContext {

	public static final String SP_DEFAULT_USERNAME = "spadmin";
	public static final String SP_DEFAULT_PASSWORD = "admin";
	
	private static Logger log = Logger.getLogger(SailPointExternalContext.class);

	private String username = SP_DEFAULT_USERNAME;
	private String password = SP_DEFAULT_PASSWORD;
	
	private SailPointContext context = null;
	private SpringStarter springStarter = null;


	public SailPointExternalContext(String username, String password) throws Exception {
		
		this();
		this.setUsername(username);
		this.setPassword(password);
		
		// Default logger config
		//Logger.getRootLogger().getLoggerRepository().resetConfiguration();
		ConsoleAppender consoleAppender = new ConsoleAppender(); //create appender
		//configure the appender
		String loggerPattern = "%p|%C{1}|%M] %m%n";
		consoleAppender.setLayout(new PatternLayout(loggerPattern)); 
		consoleAppender.setThreshold(Level.DEBUG);
		consoleAppender.activateOptions();
		//add appender to any Logger (here is root)
		Logger.getRootLogger().removeAllAppenders();
		Logger.getRootLogger().addAppender(consoleAppender);
		
		log = Logger.getLogger(this.getClass().getPackage().getName());;
		log.setLevel(Level.TRACE);

	}
	
	public SailPointExternalContext() {
		log.setLevel(Level.DEBUG);
	}
	
	public void setUsername(String value) {
		username = value;
	}
	
	public String getUsername() {
		return username;
	}

	public void setPassword(String value) {
		password = value;
	}

	public void start() throws GeneralException {

		log.trace("Getting IIQ branding.");
		String dflt = BrandingServiceFactory.getService().getSpringConfig();
		
		if (log.isTraceEnabled()) log.trace("Got branding: " + dflt);

		springStarter = new SpringStarter(dflt);

		String configFile = springStarter.getConfigFile();
		if (log.isTraceEnabled()) log.trace("Reading spring config from: " + configFile);

		String spVersion = sailpoint.Version.getVersion();
		Method method;
		try {
	
			if (spVersion.equals("5.5")) {
	            method = springStarter.getClass().getMethod("setSuppressTaskScheduler", boolean.class);
				method.invoke(springStarter, true);
				
				method = springStarter.getClass().getMethod("setSuppressRequestScheduler", boolean.class);
				method.invoke(springStarter, true);
	            
			} else 	if (spVersion.equals("6.1") || spVersion.equals("6.0")) { 
				
				method = springStarter.getClass().getMethod("setSuppressTaskScheduler", boolean.class);

				method.invoke(springStarter, true);
				method = springStarter.getClass().getMethod("setSuppressRequestScheduler", boolean.class);
				method.invoke(springStarter, true);
				method = springStarter.getClass().getMethod("setSuppressServices", boolean.class);
				method.invoke(springStarter, true);

			} else  {

				method = springStarter.getClass().getMethod(
						"addSuppressedServices", String.class);
				method.invoke(springStarter, "Task");
				method.invoke(springStarter, "Request");
				method.invoke(springStarter, "Heartbeat");

			} 
		//TODO:  Make sense of these error messages!
		} catch (NoSuchMethodException e) {
			throw new GeneralException("Could not find method, please check you have set the correct version of IIQ in setVersion method.", e);
		} catch (SecurityException e) {
			throw new GeneralException("Could not find method, please check you have set the correct version of IIQ in setVersion method.", e);
		} catch (IllegalArgumentException  e) {
			throw new GeneralException("Could not find method, please check you have set the correct version of IIQ in setVersion method.", e);
		} catch (InvocationTargetException  e) {
			throw new GeneralException("Could not find method, please check you have set the correct version of IIQ in setVersion method.", e);
		} catch (IllegalAccessException e) {
			throw new GeneralException("Could not find method, please check you have set the correct version of IIQ in setVersion method.", e);
		} 
		
		if (log.isDebugEnabled()) log.debug("Set up SpringStarter for IIQ version " + spVersion);
		
		if (log.isTraceEnabled())
			log.trace("Starting springstarter...");


		springStarter.start();

		
		if (log.isTraceEnabled())
			log.trace("Started spring starter...");
	}

	public void close() {

		if (context != null) {
			log.trace("Releasing context");
			try {
				SailPointFactory.releaseContext(context);
			} catch (GeneralException e) {
				log.error("Error when attempting to release the context", e);
			}
		}
		
		if (springStarter == null) {
			log.warn("Spring starter is null, cannot close.");
		} else {
			
			if (log.isTraceEnabled()) log.trace("Closing spring starter.");
			springStarter.close();
			if (log.isTraceEnabled()) log.trace("Spring starter is closed.");
		}

	
	}

	public SailPointContext getSailPointContext(String username, String password)
			throws GeneralException {

		setUsername(username);
		setPassword(password);
		return getSailPointContext();

	}
	

	public SailPointContext getSailPointContext() throws GeneralException {
		
		if (context == null) {
			if (log.isDebugEnabled())log.debug("Creating a context.");
			context = SailPointFactory.createContext();
			if (log.isDebugEnabled())	log.debug("Authenticating as " + username);
			
			try {
				context.authenticate(username, password);
			} catch (ExpiredPasswordException e) {
				throw new GeneralException("Error on authentication", e);
			}
			
		} else {
			log.trace("Re-using existing context.");
		}

		return context;
	}

}
