package scc.utils;

import javax.servlet.ServletContext;
import java.io.FileInputStream;
import java.util.Properties;

public class AzureProperties
{
	public static final String PROPS_FILE = "azurekeys-westeurope.props";
	private static Properties props;
	
	public static synchronized Properties getProperties(ServletContext ctx) {
		if( props == null || props.size() == 0) {
			props = new Properties();
			try {
				if( ctx == null)
					props.load( new FileInputStream("WEB-INF/" + PROPS_FILE));
				else
					props.load(ctx.getResourceAsStream("WEB-INF/" + PROPS_FILE));
			} catch (Exception e) {
				// do nothing
			}
		}
		return props;
	}
	public static synchronized Properties getProperties() {
		if( props == null || props.size() == 0) {
			props = new Properties();
			try {
				props.load( new FileInputStream("WEB-INF/" + PROPS_FILE));
			} catch (Exception e) {
				// do nothing
			}
		}
		return props;
	}

	public static String getProperty(ServletContext ctx, String key) {
		String val = null;
		try {
			val = System.getenv( key);
		} catch( Exception e) {
			// do nothing
		}
		if( val != null)
			return val;
		val = getProperties( ctx).getProperty(key);
		return val;
	}

	public static String getProperty(String key) {
		String val = null;
		try {
			val = System.getenv( key);
		} catch( Exception e) {
			// do nothing
		}
		if( val != null)
			return val;
		val = getProperties().getProperty(key);
		return val;
	}
}
