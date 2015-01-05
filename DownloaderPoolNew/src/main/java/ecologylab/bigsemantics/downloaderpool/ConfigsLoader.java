package ecologylab.bigsemantics.downloaderpool;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Loading configurations.
 * 
 * @author quyin
 */
public class ConfigsLoader
{

  public static final String CONFIG_ENV_VAR           = "BIGSEMANTICS_SERVICE_DPOOL_CONFIG";

  public static final String DEFAULT_CONFIG_FILE_NAME = "dpool-defaults.properties";

  public Configuration load(String propertyFile) throws ConfigurationException
  {
    String fn = propertyFile;
    if (fn == null || fn.length() == 0)
    {
      fn = System.getenv(CONFIG_ENV_VAR);
    }
    if (fn == null || fn.length() == 0)
    {
      fn = DEFAULT_CONFIG_FILE_NAME;
    }
    return searchConfigs(fn);
  }

  /**
   * search for propertyFile:
   * <ul>
   * <li>current directory</li>
   * <li>user home directory</li>
   * <li>classpath</li>
   * </ul>
   * 
   * @param propertyFile
   * @return Loaded Configuration.
   * @throws ConfigurationException
   */
  private Configuration searchConfigs(String propertyFile) throws ConfigurationException
  {
    return new PropertiesConfiguration(propertyFile);
  }

}
