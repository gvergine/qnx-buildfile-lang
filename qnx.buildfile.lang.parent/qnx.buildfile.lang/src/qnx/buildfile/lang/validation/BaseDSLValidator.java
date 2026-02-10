package qnx.buildfile.lang.validation;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.emf.ecore.EStructuralFeature;

import qnx.buildfile.lang.utils.JarLoader;


public class BaseDSLValidator extends AbstractBuildfileDSLValidator
{
	private static URLClassLoader classLoader;
	private static JarLoader jarLoader = new JarLoader(BaseDSLValidator.class);

	public void reportError(String message, EStructuralFeature feature, String code)
	{
		error(message, feature, code);
	}

	public void reportWarning(String message, EStructuralFeature feature, String code)
	{
		warning(message, feature, code);
	}

	public static BaseDSLValidator loadValidatorFromJar(File jarFile) throws Exception
	{
		return (BaseDSLValidator) jarLoader.loadJar(jarFile.toPath());
	}
}

