package qnx.buildfile.lang.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.service.SingletonBinding;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Guice;
import com.google.inject.Injector;

import qnx.buildfile.lang.BuildfileDSLRuntimeModule;
import qnx.buildfile.lang.BuildfileDSLStandaloneSetup;
import qnx.buildfile.lang.buildfileDSL.Model;
import qnx.buildfile.lang.validation.BuildfileDSLValidator;

public class Parser
{
	private final Injector injector;
	private final ResourceSet resourceSet;
	private final IResourceValidator validator;
	
	public Parser()
	{
		this(BuildfileDSLValidator.class);
	}
	
	public Parser(Class<? extends BuildfileDSLValidator> validatorClass)
	{
		injector = new CustomBuildfileDSLStandaloneSetup(validatorClass).createInjectorAndDoEMFRegistration();
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
	
	public static class CustomBuildfileDSLStandaloneSetup extends BuildfileDSLStandaloneSetup
	{
		private final Class<? extends BuildfileDSLValidator> validatorClass;
		
		public CustomBuildfileDSLStandaloneSetup(Class<? extends BuildfileDSLValidator> validatorClass)
		{
			super();
			this.validatorClass = validatorClass;
		}
		
		@Override
		public Injector createInjector()
		{
			return Guice.createInjector(new CustomBuildfileDSLRuntimeModule(validatorClass));
		}	
	}

	public static class CustomBuildfileDSLRuntimeModule extends BuildfileDSLRuntimeModule
	{
		private final Class<? extends BuildfileDSLValidator> validatorClass;
		
		public CustomBuildfileDSLRuntimeModule(Class<? extends BuildfileDSLValidator> validatorClass)
		{
			super();
			this.validatorClass = validatorClass;
		}
		
		@Override
		@SingletonBinding(eager=true)
		public Class<? extends BuildfileDSLValidator> bindBuildfileDSLValidator()
		{
			return validatorClass;
		}
	}
}
