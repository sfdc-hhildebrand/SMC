//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
//
// The Original Code is State Machine Compiler (SMC).
//
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2005 - 2009. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//
// RCS ID
// $Id: SmcJavaGenerator.java,v 1.11 2011/11/20 14:58:33 cwrapp Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sf.smc.Smc;
import net.sf.smc.model.*;
import net.sf.smc.model.SmcElement.TransType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visits the abstract syntax tree, emitting code based on a free marker remplate.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcFreeMarkerGenerator extends SmcCodeGenerator {

    private boolean requiresPush = false;
	private String startState;

//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

	private final SmcOptions _options;
	private boolean shouldPackageDir;

	/**
     * Creates a Java code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcFreeMarkerGenerator(final SmcOptions options)
    {
	    super( options, options.templateSuffix() );
	    shouldPackageDir = options.packageDir();
	    _options = options;

    } // end of SmcFMGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits Java code for the finite state machine.
     * @param fsm emit Java code for this finite state machine.
     */
    @Override
    public void visit(SmcFSM fsm)
    {
	    String rawSource = fsm.getSource();
	    String packageName = fsm.getPackage();
	    String context = fsm.getContext();
	    String fsmClassName = fsm.getFsmClassName();
	    startState = fsm.getStartState();
	    List<SmcMap> maps = fsm.getMaps();
	    List<SmcTransition> transitions = fsm.getTransitions();

	    requiresPush = calcRequiresPush( transitions );

	    Configuration cfg = new Configuration();

	    try
	    {
		    FileTemplateLoader ftl1;
		    TemplateLoader[] loaders;
		    //ClassTemplateLoader ctl = new ClassTemplateLoader( getClass(), "/net/sf/smc/generator/templates/" );
		    ClassTemplateLoader ctl = new ClassTemplateLoader( getClass(), "templates" );
		    ClassTemplateLoader ctl2 = new ClassTemplateLoader( getClass(), "/" );
		    ClassTemplateLoader ctl3 = new ClassTemplateLoader( Smc.class, "" );

		    if (_options.templateDirectory() != null && !_options.templateDirectory().isEmpty())
		    {
			     ftl1 = new FileTemplateLoader(new File( _options.templateDirectory()));
                 loaders= new TemplateLoader[]{ ftl1, ctl,ctl2,ctl3 };
		    }
		    else if (_options.srcDirectory() != null)
		    {
			    ftl1 = new FileTemplateLoader(new File( _options.srcDirectory()));
			    loaders = new TemplateLoader[]{ ftl1, ctl,ctl2,ctl3 };
		    }
		    else
		    {
			    loaders = new TemplateLoader[]{ ctl,ctl2,ctl3 };

		    }


		    MultiTemplateLoader mtl = new MultiTemplateLoader( loaders );
		    //System.out.println(mtl.findTemplateSource("SMCJavaTemplate.fmtl"));
		    //System.out.println(mtl.findTemplateSource("SMCJavaTemplate-Map.fmtl"));
		    cfg.setTemplateLoader( mtl );
		    cfg.setLocalizedLookup(false);



		    cfg.setObjectWrapper( new DefaultObjectWrapper() );

		    Map<String,Object> root = new HashMap<String,Object>();
		    root.put( "rawSource", rawSource );
		    root.put( "context", context );
		    root.put( "fsmClassName", fsmClassName );
		    root.put( "packageName", packageName );
		    root.put( "startState", startState );
		    root.put( "maps", maps );
		    root.put( "transitions", transitions );
		    root.put( "requiresPush", requiresPush );
		    root.put("accessLevel",_options.accessLevel());
		    root.put("reflectFlag",_options.reflectFlag());
		    root.put("genericFlag",_options.genericFlag());
		    root.put("accessLevel",_options.accessLevel());
		    root.put("noCatchFlag",_options.noCatchFlag());
		    root.put("serialFlag",_options.serialFlag());
		    root.put("srcfileBase",_options.srcfileBase());
		    root.put("noStreamsFlag",_options.noStreamsFlag());
		    root.put("castType",_options.castType());
		    root.put("debugLevel",_options.debugLevel());
		    root.put("graphLevel",_options.graphLevel());
		    root.put("syncFlag",_options.syncFlag());
		    root.put("headerDirectory",_options.headerDirectory());
		    root.put("srcDirectory",_options.srcDirectory());
		    root.put("targetfileBase",_options.targetfileBase());
		    root.put("templateDirectory",_options.templateName());
		    root.put("templateSuffix",_options.templateSuffix());
		    root.put( "fsm", fsm );
		    root.put( "generator", this );

		    Template template = cfg.getTemplate( _options.templateName() );
		    Writer out = new OutputStreamWriter( _source );
		    template.process( root, out );
		    out.flush();

	    }
	    catch ( IOException e )
	    {
		    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
	    }
	    catch ( TemplateException e )
	    {
		    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
	    }

    }


    /* (non-Javadoc)
     * @see net.sf.smc.generator.SmcCodeGenerator#appendPackageName(java.lang.String, java.lang.String)
     */
    @Override
    protected Object appendPackageName(String path, String packageName) {
	    if (shouldPackageDir)
        return (path.endsWith(File.separator) ? path : path + File.separator)
               + packageName.replace('.', File.separatorChar) + File.separator;
	    else
		    return path;
    }
    
    public String getCleanStartState()
    {
    	String javaState;
	    int index;
    	if ((index = startState.indexOf("::")) >= 0)
        {
            javaState =
                startState.substring(0, index) +
                "." +
                startState.substring(index + 2);
        }
        else
        {
            javaState = startState;
        }
    	return javaState;
    }
    
    protected boolean calcRequiresPush(List<SmcTransition> transitions)
    {

	    for (SmcTransition transition: transitions)
	    {
			for (SmcGuard guard: transition.getGuards())
			{
				if (guard.getTransType()  == TransType.TRANS_PUSH  )
					requiresPush= true;
					return true;
			}
	    }
	    requiresPush =false;
	    return false;

    }

	public boolean getRequiresPush()
	{
		return requiresPush;
	}


//---------------------------------------------------------------
// Member data
//
} // end of class SmcJavaGenerator

//