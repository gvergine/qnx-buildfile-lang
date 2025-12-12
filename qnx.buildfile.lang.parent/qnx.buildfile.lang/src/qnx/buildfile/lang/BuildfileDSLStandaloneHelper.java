package qnx.buildfile.lang;
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
import org.eclipse.xtext.diagnostics.Severity;

import com.google.inject.Injector;

import qnx.buildfile.lang.buildfileDSL.Model;

public class BuildfileDSLStandaloneHelper
{
	public final Injector injector;
	public final ResourceSet resourceSet;
	public final IResourceValidator validator;

	public BuildfileDSLStandaloneHelper()
	{
		injector = new BuildfileDSLStandaloneSetup().createInjectorAndDoEMFRegistration();
		resourceSet = injector.getInstance(XtextResourceSet.class);
		validator = injector.getInstance(IResourceValidator.class);
	}

	public ParsingResult parse(File file) throws IOException
	{
		Resource resource = resourceSet.getResource(
				org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath()),
				true
				);
		resource.load(null);
		List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		Model model = (Model) resource.getContents().get(0);
		return new ParsingResult(issues, model);
	}

	public static class ParsingResult
	{
		public final List<Issue> issues;
		public final Model model;

		public ParsingResult(List<Issue> issues, Model model)
		{
			this.issues = issues;
			this.model = model;
		}

		public boolean hasErrors()
		{
			return issues.stream().map(Issue::getSeverity).filter(Severity.ERROR::equals).count() != 0;
		}

		public boolean noErrors()
		{
			return !hasErrors();
		}
	}
}
