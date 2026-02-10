package qnx.buildfile.lang.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Injector;

import qnx.buildfile.lang.BuildfileDSLStandaloneSetup;
import qnx.buildfile.lang.buildfileDSL.Model;

/**
 * Convenience wrapper for parsing and validating buildfiles outside of
 * Eclipse / LSP (e.g. from the CLI or from tests).
 * <p>
 * Custom validation is handled transparently by the injected
 * {@link qnx.buildfile.lang.validation.CustomValidatorJarPathProvider};
 * for the CLI this is driven by the {@code -DcustomValidatorJar} system property.
 */
public class Parser
{
	private final ResourceSet resourceSet;
	private final IResourceValidator validator;

	public Parser()
	{
		Injector injector = new BuildfileDSLStandaloneSetup().createInjectorAndDoEMFRegistration();
		resourceSet = injector.getInstance(XtextResourceSet.class);
		validator = injector.getInstance(IResourceValidator.class);
	}

	public ParsingResult parse(File file) throws IOException
	{
		Resource resource = resourceSet.getResource(
				org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath()),
				true);
		resource.load(null);
		List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		Model model = (Model) resource.getContents().get(0);
		return new ParsingResult(issues, model);
	}
}
