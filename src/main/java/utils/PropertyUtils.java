package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.testng.Assert;

public class PropertyUtils
{
    private static Properties props = new Properties();

    static
    {
        String workingDir = System.getProperty("user.dir");

        try
        {
            loadPropertyFile(workingDir + File.separator + "project.properties");
        } catch (FileNotFoundException realCause)
        {
            Assert.fail("Unable to load file!", realCause);
        } catch (IOException realCause)
        {
            Assert.fail("Unable to load file!", realCause);
        }
    }

    public static void loadPropertyFile(String propertyFileName) throws FileNotFoundException, IOException
    {
        props.load(new FileInputStream(propertyFileName));
    }

    public static String getProperty(String propertyKey)
    {
        String propertyValue = props.getProperty(propertyKey.trim());

        if (propertyValue == null || propertyValue.trim().length() == 0)
        {
            // Log error message
        }

        return propertyValue;
    }

    public static void setProperty(String propertyKey, String value) throws FileNotFoundException, IOException
    {
        props.setProperty(propertyKey, value);
    }
}
