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

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
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
    /**
     * Creates a Java code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcFreeMarkerGenerator(final SmcOptions options)
    {
	    super( options, options.templateSuffix() );
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
		    cfg.setDirectoryForTemplateLoading( new File( fsm.getSourceFileName() ) );

		    cfg.setObjectWrapper( new DefaultObjectWrapper() );

		    Map<String,Object> root = new HashMap<String,Object>();
		    root.put( "rawSource", rawSource );
		    root.put( "context", context );
		    root.put( "fsmClassName", fsmClassName );
		    root.put( "package", packageName );
		    root.put( "startState", startState );
		    root.put( "maps", maps );
		    root.put( "transitions", transitions );
		    root.put( "requiresPush", requiresPush );
		    root.put( "fsm", fsm );

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
        return (path.endsWith(File.separator) ? path : path + File.separator)
               + packageName.replace('.', File.separatorChar) + File.separator;
    }
    
    private String getCleanStartState()
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
					return true;
			}
	    }
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