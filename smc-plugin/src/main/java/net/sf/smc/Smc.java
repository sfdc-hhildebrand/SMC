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
// Copyright (C) 2000 - 2008. Charles W. Rapp.
// All Rights Reserved.
//
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python, Perl code generation and examples/Perl,
//   Ruby code generation and examples/Ruby, Lua code generation
//   and examples/Lua, Groovy code generation and examples/Groovy,
//   Scala code generation and examples/Scala.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//   Toni Arnold contributed the PHP code generation and
//   examples/PHP.
//
// SMC --
//
//  State Map Compiler
//
// This class parses a state map exception, checks the code
// for semantic consistency and then generates object-oriented
// code in the user specified target language.
//
// RCS ID
// $Id: Smc.java,v 1.42 2011/11/20 16:29:53 cwrapp Exp $
//
// CHANGE LOG
// (See bottom of file.)
//

package net.sf.smc;

import net.sf.smc.generator.*;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.parser.SmcMessage;
import net.sf.smc.parser.SmcParser;
import net.sf.smc.parser.SmcParser.TargetLanguage;

import java.io.*;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Main class for the state machine compiler application.
 * This class is responsible for processing the command line
 * arguments, configuring the parser, model and generator
 * packages according to the command line and outputing the
 * results to the user. The actual work is performed by the
 * parser, model and generator packages.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class Smc
{
//---------------------------------------------------------------
// Member Methods
//

    //-----------------------------------------------------------
    // Main method.
    //

    /**
     * The state machine compiler main method.
     * @param args command line arguments.
     */
    public static void main(final String[] args)
    {
        int retcode = 0;

        _errorMsg = new String();

        // The default smc output level is 1.
        _targetLanguage = null;
        _version = VERSION;
        _debugLevel = SmcCodeGenerator.NO_DEBUG_OUTPUT;
        _nostreams = false;
        _sync = false;
        _noex = false;
        _nocatch = false;
        _serial = false;
        _castType = "dynamic_cast";
        _graphLevel = SmcCodeGenerator.GRAPH_LEVEL_0;
        _sourceFileList = new ArrayList<String>();
        _verbose = false;
        _fsmVerbose = false;
        _return = false;
        _reflection = false;
        _outputDirectory = null;
        _headerDirectory = null;
        _suffix = null;
        _hsuffix = null;
        _accessLevel = null;
	    _templateName = null;
		_templateSuffix=null;
	    _templateDirectory=null;

        // Process the command line.
        if (parseArgs(args) == false)
        {
            retcode = 1;
            System.err.println(APP_NAME + ": " + _errorMsg);
        }
        // Arguments check out - start compiling..
        else
        {
            SmcParser parser;
            SmcFSM fsm;
            Iterator<String> sit;
            long startTime = 0;
            long finishTime;
            long totalStartTime = 0;
            long totalFinishTime;

            if (_verbose == true)
            {
                totalStartTime = System.currentTimeMillis();
            }

            try
            {
                for (sit = _sourceFileList.iterator();
                     sit.hasNext() == true;
                    )
                {
                    _sourceFileName = sit.next();

                    if (_verbose == true)
                    {
                        System.out.print("[parsing started ");
                        System.out.print(_sourceFileName);
                        System.out.println("]");

                        startTime = System.currentTimeMillis();
                    }

                    parser =
                        new SmcParser(
                            _getFileName(_sourceFileName),
                            new FileInputStream(_sourceFileName),
                            _targetLanguage.language(),
                            _fsmVerbose);

                    // First - do the parsing
                    fsm = parser.parse();

                    if (_verbose == true)
                    {
                        finishTime = System.currentTimeMillis();

                        System.out.print("[parsing completed ");
                        System.out.print(finishTime - startTime);
                        System.out.println("ms]");
                    }

					if ( parser.getMessages().size() > 0 )
					{
                        // Output the parser's messages.
                        _outputMessages(_sourceFileName,
                                        System.err,
                                        parser.getMessages());
					}
                    if (fsm == null)
                    {
                        retcode = 1;
                    }
                    else
                    {
                        SmcSyntaxChecker checker =
                            new SmcSyntaxChecker(
                                _sourceFileName,
                                _targetLanguage.language());

                        if (_verbose == true)
                        {
                            System.out.print("[checking ");
                            System.out.print(_sourceFileName);
                            System.out.println("]");
                        }

                        // Second - do the semantic check.
                        fsm.accept(checker);
                        if ( checker.getMessages().size() > 0)
                        {
                            _outputMessages(
                                _sourceFileName,
                                System.err,
                                checker.getMessages());
                        }
                        if (checker.isValid() == false)
                        {
                            retcode = 1;
                        }
                        else
                        {
                            // Third - do the code generation.
                            _generateCode(fsm);
                        }
                    }
                }
            }
            // Report an unknown file exception.
            catch (FileNotFoundException filex)
            {
                System.err.print(_sourceFileName);
                System.err.print(": error - ");
                System.err.println(filex.getMessage());
            }
            // A parse exception may be thrown by generateCode().
            // This is not a problem.
            catch (ParseException parsex)
            {
                System.err.print(_sourceFileName);
                System.err.print(":");
                System.err.print(parsex.getErrorOffset());
                System.err.print(": error - ");
                System.err.println(parsex.getMessage());
            }
            catch (Exception e)
            {
                retcode = 1;

                System.err.println(
                    "SMC has experienced a fatal error. Please e-mail the following error output to rapp@acm.org. Thank you.\n");
                System.err.println(
                    "--------------------------------------------------------------------------------");
                System.err.println("SMC version: " + _version);
                System.err.println(
                    "JRE version: v. " +
                    System.getProperty("java.version"));
                System.err.println(
                    "JRE vender: " +
                    System.getProperty("java.vendor") +
                    " (" +
                    System.getProperty("java.vendor.url") +
                    ")");
                System.err.println(
                    "JVM: " +
                    System.getProperty("java.vm.name") +
                    ", v. " +
                    System.getProperty("java.vm.version"));
                System.err.println(
                    "JVM vender: " +
                    System.getProperty("java.vm.vendor"));
                System.err.println("Exception:\n");
                e.printStackTrace();
                System.err.println(
                    "--------------------------------------------------------------------------------");
            }

            if (_verbose == true)
            {
                totalFinishTime = System.currentTimeMillis();

                System.out.print("[total ");
                System.out.print(
                    totalFinishTime - totalStartTime);
                System.out.println("ms]");
            }
        }

        // Need to return the appropriate exit code in case SMC
        // is called by make. Just doing a return always results
        // in a zero return code.
        // v. 4.0.0: But calling exit when SMC is an ANT task is
        // problematic. ANT is a Java program and calls Smc.main
        // directly and not as a forked process. So when Smc.main
        // exits, it exits the JVM for everyone including ANT.
        if (_return == false)
        {
            System.exit(retcode);
        }
        else
        {
            return;
        }
    } // end of main(String[])

    //
    // end of Main method.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Constructors.
    //

    // Private to prevent instantiation.
    private Smc()
    {}

    //
    // end of Constructors.
    //-----------------------------------------------------------

    // Parse the command line arguments and fill in the static
    // data accordingly.
    private static boolean parseArgs(final String[] args)
    {
        int i;
        int argsConsumed;
        boolean helpFlag = false;
        boolean retcode = true;

        // Look for either -help or -verson first. If specified,
        // then output the necessary info and return.
        helpFlag = _needHelp(args);
        if (helpFlag == false)
        {
            // Look for the target language second. Verify that
            // exactly one target language is specifed.
            try
            {
                _targetLanguage = _findTargetLanguage(args);
            }
            catch (IllegalArgumentException argex)
            {
                retcode = false;
                _errorMsg = argex.getMessage();
            }

            if (retcode == true && _targetLanguage == null)
            {
                retcode = false;
                _errorMsg = "Target language was not specified.";
            }
        }

        // Parse all options first. Keep going until an error is
        // encountered or there are no more options left.
        for (i = 0, argsConsumed = 0;
             i < args.length &&
                 helpFlag == false &&
                 retcode == true &&
                 args[i].startsWith("-") == true;
             i += argsConsumed, argsConsumed = 0)
        {
            // Ignore the target language flags - they have
            // been processed.
            if (_findLanguage(args[i]) != null)
            {
                argsConsumed = 1;
            }
            else if (args[i].startsWith("-ac") == true)
            {
                // -access should be followed by a string.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg =
                        ACCESS_FLAG +
                        " not followed by an access keyword";
                }
                else if (_supportsOption(ACCESS_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        ACCESS_FLAG +
                        ".";
                }
                else if (_isValidAccessLevel(args[i+1]) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support access level" +
                        args[i+1] +
                        ".";
                }
                else
                {
                    _accessLevel = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-sy") == true)
            {
                if (_supportsOption(SYNC_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        SYNC_FLAG +
                        ".";
                }
                else
                {
                    _sync = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-su") == true)
            {
                // -suffix should be followed by a suffix.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg =
                        SUFFIX_FLAG + " not followed by a value";
                }
                else if (_supportsOption(SUFFIX_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        SUFFIX_FLAG +
                        ".";
                }
                else
                {
                    _suffix = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-hs") == true)
            {
                // -hsuffix should be followed by a suffix.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg =
                        HEADER_SUFFIX_FLAG +
                        " not followed by a value";
                }
                else if (_supportsOption(
                             HEADER_SUFFIX_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        HEADER_SUFFIX_FLAG +
                        ".";
                }
                else
                {
                    _hsuffix = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].startsWith("-ts") == true)
            {
	            // -tsuffix should be followed by a suffix.
	            if ((i + 1) == args.length ||
			                args[i+1].startsWith("-") == true)
	            {
		            retcode = false;
		            _errorMsg =
				            TEMPLATE_SUFFIX_FLAG +
						            " not followed by a value";
	            }
	            else if (_supportsOption(
			                                    TEMPLATE_SUFFIX_FLAG) == false)
	            {
		            retcode = false;
		            _errorMsg =
				            _targetLanguage.name() +
						            " does not support " +
						            TEMPLATE_SUFFIX_FLAG +
						            ".";
	            }
	            else
	            {
		            _templateSuffix = args[i+1];
		            argsConsumed = 2;
	            }
            }
            else if (args[i].startsWith("-td") == true)
            {
	            // -tdir should be followed by a directory.
	            if ((i + 1) == args.length ||
			                args[i+1].startsWith("-") == true)
	            {
		            retcode = false;
		            _errorMsg =
				            TEMPLATE_DIR_FLAG +
						            " not followed by a value";
	            }
	            else if (_supportsOption(
			                                    TEMPLATE_DIR_FLAG) == false)
	            {
		            retcode = false;
		            _errorMsg =
				            _targetLanguage.name() +
						            " does not support " +
						            TEMPLATE_DIR_FLAG +
						            ".";
	            }
	            else
	            {
		            _templateDirectory = args[i+1];
		            argsConsumed = 2;
	            }
            }
            else if (args[i].startsWith("-template") == true)
            {
	            // -template should be followed by a suffix.
	            if ((i + 1) == args.length ||
			                args[i+1].startsWith("-") == true)
	            {
		            retcode = false;
		            _errorMsg =
				            TEMPLATE_NAME_FLAG +
						            " not followed by a value";
	            }
	            else if (_supportsOption(
			                                    TEMPLATE_NAME_FLAG) == false)
	            {
		            retcode = false;
		            _errorMsg =
				            _targetLanguage.name() +
						            " does not support " +
						            TEMPLATE_NAME_FLAG +
						            ".";
	            }
	            else
	            {
		            _templateName = args[i+1];
		            argsConsumed = 2;
	            }
            }
            else if (args[i].startsWith("-pd") == true)
            {
	            if (_supportsOption(PACKAGE_DIR_FLAG) == false)
	            {
		            retcode = false;
		            _errorMsg =
				            _targetLanguage.name() +
						            " does not support " +
						            PACKAGE_DIR_FLAG +
						            ".";
	            }
	            else
	            {
		            _packageDir = true;
		            argsConsumed = 1;
	            }
            }
            else if (args[i].startsWith( "-ca" ) == true)
            {
                // -cast should be followed by a cast type.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg =
                        CAST_FLAG +
                        " not followed by a value";
                }
                else if (_supportsOption(CAST_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        CAST_FLAG +
                        ".";
                }
                else if (_isValidCast(args[i+1]) == false)
                {
                    retcode = false;
                    _errorMsg =
                        "\"" +
                        args[i+1] +
                        "\" is an invalid C++ cast type.";
                }
                else
                {
                    _castType = args[i+1];
                    argsConsumed = 2;
                }
            }
            else if (args[i].equals( "-d" ) == true)
            {
                // -d should be followed by a directory.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg =
                        DIRECTORY_FLAG +
                        " not followed by directory";
                }
                else if (
                    _supportsOption(DIRECTORY_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        DIRECTORY_FLAG +
                        ".";
                }
                else
                {
                    _outputDirectory = args[i+1];
                    argsConsumed = 2;

                    // If the output directory does not end with
                    // file path separator, then add one.
                    if (_outputDirectory.endsWith(
                            File.separator) == false)
                    {
                        _outputDirectory =
                            _outputDirectory + File.separator;
                    }

                    retcode =
                        _isValidDirectory(_outputDirectory);
                }
            }
            else if (args[i].startsWith("-hea") == true)
            {
                // -headerd should be followed by a directory.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg = HEADER_FLAG +
                                " not followed by directory";
                }
                else if (
                    _supportsOption(HEADER_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        HEADER_FLAG +
                        ".";
                }
                else
                {
                    _headerDirectory = args[i+1];
                    argsConsumed = 2;

                    // If the output directory does not end with
                    // file path separator, then add one.
                    if (_headerDirectory.endsWith(
                            File.separator) == false)
                    {
                        _headerDirectory =
                            _headerDirectory + File.separator;
                    }

                    retcode =
                        _isValidDirectory(_headerDirectory);
                }
            }
            else if (args[i].startsWith( "-gl" ) == true)
            {
                // -glevel should be followed by an integer.
                if ((i + 1) == args.length ||
                    args[i+1].startsWith("-") == true)
                {
                    retcode = false;
                    _errorMsg =
                        GLEVEL_FLAG +
                        " not followed by integer";
                }
                else if (_supportsOption(GLEVEL_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        GLEVEL_FLAG +
                        ".";
                }
                else
                {
                    try
                    {
                        _graphLevel =
                            Integer.parseInt(args[i+1]);

                        if (_graphLevel < SmcCodeGenerator.GRAPH_LEVEL_0 ||
                            _graphLevel > SmcCodeGenerator.GRAPH_LEVEL_2)
                        {
                            retcode = false;
                            _errorMsg =
                                GLEVEL_FLAG +
                                " must be 0, 1 or 2";
                        }
                        else
                        {
                            argsConsumed = 2;
                        }
                    }
                    catch (NumberFormatException numberex)
                    {
                        retcode = false;

                        _errorMsg =
                            GLEVEL_FLAG +
                            " not followed by valid integer";
                    }
                }
            }
            else if (args[i].equals("-g") == true)
            {
                if (_supportsOption(DEBUG_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        DEBUG_FLAG +
                        ".";
                }
                else
                {
                    _debugLevel = SmcCodeGenerator.DEBUG_LEVEL_0;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals("-g0") == true)
            {
                if (_supportsOption(DEBUG_LEVEL0_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        DEBUG_LEVEL0_FLAG +
                        ".";
                }
                else
                {
                    _debugLevel = SmcCodeGenerator.DEBUG_LEVEL_0;
                    argsConsumed = 1;
                }
            }
            else if (args[i].equals( "-g1" ) == true)
            {
                if (_supportsOption(DEBUG_LEVEL1_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        DEBUG_LEVEL1_FLAG +
                        ".";
                }
                else
                {
                    _debugLevel = SmcCodeGenerator.DEBUG_LEVEL_1;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-nos") == true)
            {
                if (_supportsOption(NO_STREAMS_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        NO_STREAMS_FLAG +
                        ".";
                }
                else
                {
                    _nostreams = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-noe") == true)
            {
                if (_supportsOption(NO_EXCEPTIONS_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        NO_EXCEPTIONS_FLAG +
                        ".";
                }
                else
                {
                    _noex = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-noc") == true)
            {
                if (_supportsOption(NO_CATCH_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        NO_CATCH_FLAG +
                        ".";
                }
                else
                {
                    _nocatch = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-ret") == true)
            {
                if (_supportsOption(RETURN_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        RETURN_FLAG +
                        ".";
                }
                else
                {
                    _return = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-ref") == true)
            {
                if (_supportsOption(REFLECT_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        REFLECT_FLAG +
                        ".";
                }
                else
                {
                    _reflection = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-ge") == true)
            {
                if (_supportsOption(GENERIC_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        GENERIC_FLAG +
                        ".";
                }
                else
                {
                    _generic = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-se") == true)
            {
                if (_supportsOption(SERIAL_FLAG) == false)
                {
                    retcode = false;
                    _errorMsg =
                        _targetLanguage.name() +
                        " does not support " +
                        SERIAL_FLAG +
                        ".";
                }
                else
                {
                    _serial = true;
                    argsConsumed = 1;
                }
            }
            else if (args[i].startsWith("-verb") == true)
            {
                _verbose = true;
                argsConsumed = 1;
            }
            else if (args[i].startsWith("-vverb") == true)
            {
                _fsmVerbose = true;
                argsConsumed = 1;
            }
            else
            {
                retcode = false;
                _errorMsg = "Unknown option (" +
                             args[i] +
                             ")";
            }
        }

        // Was a state map source file given? It must be the
        // last argument in the list.
        if (helpFlag == false && retcode == true)
        {
            if (i == args.length)
            {
                retcode = false;
                _errorMsg = "Missing source file";
            }
            else
            {
                File sourceFile;
                String fileName;

                for (; i < args.length && retcode == true; ++i)
                {
                    // The file name must end in ".sm".
                    if (args[i].toLowerCase().endsWith(".sm") ==
                            false)
                    {
                        retcode = false;
                        _errorMsg =
                            "Source file name must end in " +
                            "\".sm\" (" +
                            args[i] +
                            ")";
                    }
                    else
                    {
                        sourceFile = new File(args[i]);
                        if (sourceFile.exists() == false)
                        {
                            retcode = false;
                            _errorMsg = "No such file named \"" +
                                         args[i] +
                                         "\"";
                        }
                        else if (sourceFile.canRead() == false)
                        {
                            retcode = false;
                            _errorMsg = "Source file \"" +
                                         args[i] +
                                         "\" is not readable";
                        }
                        else
                        {
                            // Normalize the file name. If the /
                            // file name separator is used and
                            // this is Windows, then replace with
                            // \.
                            fileName = args[i];
                            if (File.separatorChar != '/')
                            {
                                String fileSeparator =
                                    Matcher.quoteReplacement(
                                        File.separator);

                                fileName =
                                    fileName.replaceAll(
                                        "/", fileSeparator);
                            }

                            _sourceFileList.add(fileName);
                        }
                    }
                }
            }
        }

        return (retcode);
    } // end of parseArgs(String[])

    // Process the -help and -version flags separately.
    private static boolean _needHelp(final String[] args)
    {
        int i;
        boolean retval = false;

        for (i = 0; i < args.length && retval == false; ++i)
        {
            if (args[i].startsWith("-hel") == true)
            {
                retval = true;
                _usage(System.out);
            }
            else if (args[i].startsWith("-vers") == true)
            {
                retval = true;
                System.out.println(APP_NAME + " " + _version);
            }
        }

        return (retval);
    } // end of _needHelp(String[])

    // Returns the target language found in the command line
    // arguments. Throws an IllegalArgumentException if more than
    // one target language is specified.
    // As a side effect sets the default suffix.
    private static Language _findTargetLanguage(
        final String[] args)
    {
        int i;
        Language lang;
        Language retval = null;

        for (i = 0; i < args.length; ++i)
        {
            // Is this argument a language name?
            if ((lang = _findLanguage(args[i])) != null)
            {
                // Only one target langugage can be specified.
                if (retval != null && retval != lang)
                {
                    throw (
                        new IllegalArgumentException(
                            "Only one target language " +
                            "may be specified"));
                }
                else
                {
                    retval = lang;
                }
            }
        }

        return (retval);
    } // end of _findTargetLanguage(String[])

    // Returns the langugage record associated with the given
    // command line option.
    private static Language _findLanguage(final String option)
    {
        int index;
        Language retval = null;

        for (index = 1;
             index < _languages.length && retval == null;
             ++index)
        {
            if (option.equals(
                    _languages[index].optionFlag()) == true)
            {
                retval = _languages[index];
            }
        }

        return (retval);
    } // end of _findLanguage(String)

    // Returns true if the target language supports the specified
    // option.
    private static boolean _supportsOption(final String option)
    {
        List<Language> languages = _optionMap.get(option);

        return (
            languages != null &&
            languages.contains(_targetLanguage));
    } // end of _supportsOption(String)

    // Returns true if the string is a valid access level for
    // the target language.
    private static boolean _isValidAccessLevel(final String s)
    {
        boolean retcode =
            _accessMap.containsKey(_targetLanguage);

        if (retcode == true)
        {
            List<String> levels =
                _accessMap.get(_targetLanguage);

            retcode = levels.contains(s);
        }

        return (retcode);
    } // end of _isValidAccessLevel(String)

    // Returns true if the string is a valid C++ cast.
    private static boolean _isValidCast(final String castType)
    {
        return (castType.equals("dynamic_cast") == true ||
                castType.equals("static_cast") == true ||
                castType.equals("reinterpret_cast") == true);
    } // end of _isValidCast(String)

    // Returns true if the path is a valid destination directory.
    private static boolean _isValidDirectory(final String path)
    {
        boolean retcode = false;

        try
        {
            File pathObj = new File(path);

            if (pathObj.isDirectory() == false)
            {
                _errorMsg =
                    "\"" + path + "\" is not a directory";
            }
            else if (pathObj.canWrite() == false)
            {
                _errorMsg =
                    "\"" + path + "\" is not writeable";
            }
            else
            {
                retcode = true;
            }
        }
        catch (SecurityException securex)
        {
            _errorMsg = "Unable to access \"" + path + "\"";
        }

        return (retcode);
    } // end of _isValidDirectory(String)

    private static void _usage(final PrintStream stream)
    {
        stream.print("usage: ");
        stream.print(APP_NAME);
        stream.print(" [-access level]");
        stream.print(" [-suffix suffix]");
        stream.print(" [-g | -g0 | -g1]");
        stream.print(" [-nostreams]");
        stream.print(" [-version]");
        stream.print(" [-verbose]");
        stream.print(" [-help]");
        stream.print(" [-sync]");
        stream.print(" [-noex]");
        stream.print(" [-nocatch]");
        stream.print(" [-serial]");
        stream.print(" [-return]");
        stream.print(" [-reflect]");
        stream.print(" [-generic]");
        stream.print(" [-cast cast_type]");
        stream.print(" [-d directory]");
        stream.print(" [-headerd directory]");
	    stream.print(" [-hsuffix suffix]");
	    stream.print(" [-template template]");
	    stream.print(" [-tsuffix suffix]");
	    stream.print(" [-glevel int]");
        stream.print(
            " {-c | -c++ | -csharp | -graph | -groovy | -java | -js ");
        stream.print(
            "-lua | -objc | -perl | -php | -python | -ruby | ");
        stream.print("-scala | -table |-tcl | -vb | -fm }");
        stream.println(" statemap_file");
        stream.println("    where:");
        stream.println(
            "\t-access   Use this access keyword for the generated classes");
        stream.println("\t          (use with -java only)");
        stream.println(
            "\t-suffix   Add this suffix to output file");
        stream.println(
            "\t-g, -g0   Add level 0 debugging output to generated code");
        stream.println(
            "\t          (output for entering, exiting states and transitions)");
        stream.println(
            "\t-g1       Add level 1 debugging output to generated code");
        stream.println(
            "\t          (level 0 output plus state Entry and Exit actions)");
        stream.println("\t-nostreams Do not use C++ iostreams ");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.print("\t-version  Print smc version ");
        stream.println("information to standard out and exit");
        stream.print("\t-verbose  ");
        stream.println("Output more compiler messages.");
        stream.print("\t-help     Print this message to ");
        stream.println("standard out and exit");
        stream.println(
            "\t-sync     Synchronize access to transition methods");
        stream.print("\t          ");
        stream.println("(use with -csharp, -java, -groovy, -scala and -vb only)");
        stream.println(
            "\t-noex     Do not generate C++ exception throws ");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.print(
            "\t-nocatch  Do not generate try/catch/rethrow ");
        stream.println("code (not recommended)");
        stream.println(
            "\t-serial   Generate serialization code");
        stream.print("\t-return   ");
        stream.println("Smc.main() returns, not exits");
        stream.print("\t          ");
        stream.println("(use this option with ANT)");
        stream.println("\t-reflect  Generate reflection code");
        stream.print("\t          ");
        stream.print("(use with -csharp, -groovy, -java, -lua,");
        stream.print(" -perl, -php, -python, -ruby, -scala, ");
        stream.println("-tcl and -vb only)");
        stream.println("\t-generic  Use generic collections");
        stream.print("\t          ");
        stream.println("(use with -csharp, -java or -vb and -reflect only)");
        stream.println("\t-cast     Use this C++ cast type ");
        stream.print("\t          ");
        stream.println("(use with -c++ only)");
        stream.println(
            "\t-d        Place generated files in directory");
        stream.print(
            "\t-headerd  Place generated header files in ");
        stream.println("directory");
        stream.print("\t          ");
        stream.println("(use with -c, -c++, -objc only)");
        stream.println(
            "\t-hsuffix  Add this suffix to output header file");
        stream.print("\t          ");
        stream.println("(use with -c, -c++, -objc only)");
        stream.print(
            "\t-glevel   Detail level from 0 (least) to 2 ");
        stream.println("(greatest)");
        stream.print("\t          ");
        stream.println("(use with -graph only)");
        stream.println("\t-c        Generate C code");
        stream.println("\t-c++      Generate C++ code");
        stream.println("\t-csharp   Generate C# code");
        stream.println("\t-graph    Generate GraphViz DOT file");
        stream.println("\t-groovy   Generate Groovy code");
        stream.println("\t-java     Generate Java code");
        stream.println("\t-js       Generate JavaScript code");
        stream.println("\t-lua      Generate Lua code");
        stream.println("\t-objc     Generate Objective-C code");
        stream.println("\t-perl     Generate Perl code");
        stream.println("\t-php      Generate PHP code");
        stream.println("\t-python   Generate Python code");
        stream.println("\t-ruby     Generate Ruby code");
        stream.println("\t-scala    Generate Scala code");
        stream.println("\t-table    Generate HTML table code");
        stream.println("\t-tcl      Generate [incr Tcl] code");
	    stream.println("\t-vb       Generate VB.Net code");
	    stream.println("\t-fm       Use FreeMarker template");
	    stream.println(
			                  "\t\t-template  <name> use this template name");
	    stream.println(
			                  "\t-tsuffix   <suffix> use this suffix on generated code");
	    stream.println();
        stream.println(
            "    Note: statemap_file must end in \".sm\"");
        stream.print(
            "    Note: must select one of -c, -c++, -csharp, ");
        stream.print("-graph, -groovy, -java, -lua, -objc, -perl, ");
        stream.println(
            "-php, -python, -ruby, -scala, -table, -tcl or -vb.");

        return;
    } // end of _usage(PrintStream)

    // Returns the <name> portion from <path>/<name>.sm.
    private static String _getFileName(String fullName)
    {
        File file = new File(fullName);
        String fileName = file.getName();

        // Note: this works because the file name's form
        // has already been validated as ending in .sm.
        return (
            fileName.substring(
                0, fileName.toLowerCase().indexOf(".sm")));
    } // end of _getFileName(String)

    // Generates the State pattern in the target language.
    private static void _generateCode(final SmcFSM fsm)
        throws FileNotFoundException,
               IOException,
               ParseException
    {
        int endIndex =
            _sourceFileName.length() - 3;
        String srcFilePath =
            "." + System.getProperty("file.separator");
        String srcFileBase = fsm.getTargetFileName();
        String headerPath;
        String headerFileName = "";
        FileOutputStream headerFileStream = null;
        PrintStream headerStream = null;
        SmcCodeGenerator headerGenerator = null;
        String srcFileName = "";
        FileOutputStream sourceFileStream = null;
        PrintStream sourceStream = null;
        SmcOptions options = null;
        SmcCodeGenerator generator = null;

        // For some strange reason I get the wrong
        // line separator character when I use Java
        // on Windows. Set the line separator to "\n"
        // and all is well.
        System.setProperty("line.separator", "\n");

        // Strip away any preceding directories from
        // the source file name.
        endIndex = _sourceFileName.lastIndexOf(File.separatorChar);
        if (endIndex >= 0)
        {
            srcFilePath =
                _sourceFileName.substring(
                    0, (endIndex + 1));
        }

        // If -d was specified, then use place generated file
        // there.
        if (_outputDirectory != null)
        {
            srcFilePath = _outputDirectory;
        }

        // If -headerd was specified, then place the file
        // there. -headerd takes precedence over -d.
        if (_headerDirectory != null)
        {
            headerPath = _headerDirectory;
        }
        else
        {
            headerPath = srcFilePath;
        }

        if (_accessLevel == null)
        {
            _accessLevel = "public";
        }
        else if (_accessLevel.equals(PACKAGE_LEVEL) == true)
        {
            _accessLevel = "/* package */";
        }

        options = new SmcOptions(fsm.getSourceFileName(),
                                 srcFileBase,
                                 _outputDirectory,
                                 _headerDirectory,
                                 _castType,
                                 _graphLevel,
                                 _serial,
                                 _debugLevel,
                                 _noex,
                                 _nocatch,
                                 _nostreams,
                                 _reflection,
                                 _sync,
                                 _generic,
                                 _accessLevel,
		                                _templateName,
		                         _templateDirectory,
		                                _templateSuffix, _packageDir);

        // Create the header file name and generator -
        // if the language uses a header file.
        if (_targetLanguage.hasHeaderFile() == true)
        {
            headerGenerator =
                _targetLanguage.headerGenerator(options);
            headerFileName =
                headerGenerator.sourceFile(
                    headerPath, null, srcFileBase, null);
            headerFileStream =
                new FileOutputStream(headerFileName);
            headerStream =
                new PrintStream(headerFileStream);
            headerGenerator.setSource(headerStream);
        }

        // Create the language-specific source code generator.
        generator = _targetLanguage.generator(options);
        srcFileName =
            generator.sourceFile(
                srcFilePath, fsm.getPackage(), srcFileBase, _suffix);
        File srcFile = new File(srcFileName);
        srcFile.getParentFile().mkdirs();
        sourceFileStream =
            new FileOutputStream(srcFileName);
        sourceStream =
            new PrintStream(sourceFileStream);
        generator.setSource(sourceStream);

        // Generate the header file first.
        if (headerGenerator != null)
        {
            fsm.accept(headerGenerator);
            headerFileStream.flush();
            headerFileStream.close();

            if (_verbose == true)
            {
                System.out.print("[wrote ");
                System.out.print(headerFileName);
                System.out.println("]");
            }
        }

        // Now output the FSM in the target language.
        if (generator != null)
        {
            fsm.accept(generator);
            sourceFileStream.flush();
            sourceFileStream.close();

            if (_verbose == true)
            {
                System.out.print("[wrote ");
                System.out.print(srcFileName);
                System.out.println("]");
            }
        }

        return;
    } // end of _generateCode(SmcFSM)

    // Outputs parser warning and error messages concerning the
    // named .sm file to the provided stream.
    private static void _outputMessages(
        final String srcFileName,
        final PrintStream stream,
        final List<SmcMessage> messages)
    {
        for (SmcMessage message: messages)
        {
            stream.print(srcFileName);
            stream.print(':');
            stream.print(message.getLineNumber());

            if (message.getLevel() == SmcMessage.WARNING)
            {
                stream.print(": warning - ");
            }
            else
            {
                stream.print(": error - ");
            }

            stream.println(message.getText());
        }

        return;
    } // end of _outputMessages(String, PrintStream, List<>)

//---------------------------------------------------------------
// Inner classes
//

    // The Language class explicitly stores each target
    // language's properties:
    // + The *start* of the command line option.
    // + The language's full name.
    // + The language's SmcCodeGenerator subclass.
    // + Whether the language also generates a header file and
    //   that header file SmcCodeGenerator subclass.

    @SuppressWarnings("rawtypes")
    /* package */ static final class Language
    {
    //-----------------------------------------------------------
    // Member methods.
    //

        //-------------------------------------------------------
        // Constructors.
        //

        @SuppressWarnings("unchecked")
        public Language(final TargetLanguage language,
                        final String optionFlag,
                        final String name,
                        final Class generator,
                        final Class headerGenerator)
        {
            Constructor sourceCtor = null;
            Constructor headerCtor = null;

            _language = language;
            _optionFlag = optionFlag;
            _name = name;

            if (generator != null)
            {
                try
                {
                    sourceCtor =
                        generator.getDeclaredConstructor(
                            SmcOptions.class);
                }
                catch (NoSuchMethodException methodex)
                {}
            }

            if (headerGenerator != null)
            {
                try
                {
                    headerCtor =
                        headerGenerator.getDeclaredConstructor(
                            SmcOptions.class);
                }
                catch (NoSuchMethodException methoex)
                {}
            }

            _generator = sourceCtor;
            _headerGenerator = headerCtor;
        } // end of Language(...)

        //
        // end of Constructors.
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Get methods.
        //

        public TargetLanguage language()
        {
            return (_language);
        } // end of language()

        public String optionFlag()
        {
            return (_optionFlag);
        } // end of optionFlag()

        public String name()
        {
            return (_name);
        } // end of name()

        public SmcCodeGenerator generator(
            final SmcOptions options)
        {
            SmcCodeGenerator retval = null;

            try
            {
                retval =
                    (SmcCodeGenerator)
                        _generator.newInstance(options);
            }
            catch (Exception jex)
            {
                System.err.print(options.srcfileBase());
                System.err.print(".sm: failed to create ");
                System.err.print(_language);
                System.err.println(" generator:");
                jex.printStackTrace();
            }

            return (retval);
        } // end of generator(SmcOptions)

        public boolean hasHeaderFile()
        {
            return (_headerGenerator != null);
        } // end of hasHeaderFile()

        public SmcCodeGenerator headerGenerator(
            final SmcOptions options)
        {
            SmcCodeGenerator retval = null;

            try
            {
                retval =
                    (SmcCodeGenerator)
                        _headerGenerator.newInstance(options);
            }
            catch (Exception jex)
            {
                // Ignore. Return null.
            }

            return (retval);
        } // end of headerGenerator(Options)

        //
        // end of Get methods.
        //-------------------------------------------------------

        @Override
        public boolean equals(final Object o)
        {
            boolean retcode = (o == this);

            if (retcode == false && o instanceof Language)
            {
                retcode =
                    (_language == ((Language) o)._language);
            }

            return (retcode);
        } // end of equals(Object)

        @Override
        public int hashCode()
        {
            return (_language.ordinal());
        } // end of hashCode()
            
        @Override
        public String toString()
        {
            return (_name);
        } // end of toString()

    //-----------------------------------------------------------
    // Member data.
    //

        private final TargetLanguage _language;
        private final String _optionFlag;
        private final String _name;
        private final Constructor _generator;
        private final Constructor _headerGenerator;
    } // end of class Language

//---------------------------------------------------------------
// Member Data
//

    //-----------------------------------------------------------
    // Statics.
    //

    // The source file currently being compiled.
    private static String _sourceFileName;

    // The state map source code to be compiled.
    private static List<String> _sourceFileList;

    // Append this suffix to the end of the output file.
    private static String _suffix;

    // Append this suffix to the end of the output header file.
    @SuppressWarnings("unused")
    private static String _hsuffix;

    // Place the output files in this directory. May be null.
    private static String _outputDirectory;

    // Place header files in this directory. May be null.
    private static String _headerDirectory;

    // The debug level.
    private static int _debugLevel;

    // If true, then do not use C++ iostreams for debugging.
    // Application code must provide a TRACE macro to output
    // the debug messages.
    private static boolean _nostreams;

    // If true, then generate thread-safe Java code.
    private static boolean _sync;

    // If true, then do *not* generate C++ exception throws.
    private static boolean _noex;

    // If true, then do *not* generate try/catch/rethrow code.
    private static boolean _nocatch;

    // If true, then generate unique integer IDs for each state.
    private static boolean _serial;

    // If true, then generate getTransitions() method for each
    // state.
    private static boolean _reflection;

    // If true, then use a Map<String, Integer> for the
    // reflection map.
    private static boolean _generic;

    // If true, then generate compiler verbose messages.
    private static boolean _verbose;

    // If true, then generate FSM messages.
    private static boolean _fsmVerbose;

    // The details placed into the GraphViz DOT file.
    private static int _graphLevel;

    // When generating C++ code, use this cast type.
    private static String _castType;

    // Have Smc.main() return rather than exit.
    private static boolean _return;

    // Use this access identifier for the generated classes.
    private static String _accessLevel;

    // Store command line error messages here.
    private static String _errorMsg;

    // The app's version ID.
    private static String _version;

    // The list of all supported languages.
    private static Language[] _languages;

    // Maps each command line option flag to the target languages
    // supporting the flag.
    // private static Map<String, List<Language>> _optionMap;
    private static Map<String, List<Language>> _optionMap;

    // Maps the target language to the list of acceptable access
    // levels.
    private static Map<Language, List<String>> _accessMap;

	// parameters for template generation. The name of the template, the file extension to generate
	// and the directory to search for files
	private static String _templateName;
	private static String _templateSuffix;
	private static String _templateDirectory;
	private static boolean _packageDir;

	//-----------------------------------------------------------
    // Constants.
    //

    // Specifies target programming language.
    /* package */ static Language _targetLanguage;

    private static final String APP_NAME = "smc";
    private static final String VERSION = "v. 6.1.0";

    // Command line option flags.
    private static final String ACCESS_FLAG = "-access";
    private static final String CAST_FLAG = "-cast";
    private static final String DIRECTORY_FLAG = "-d";
    private static final String DEBUG_FLAG = "-g";
    private static final String DEBUG_LEVEL0_FLAG = "-g0";
    private static final String DEBUG_LEVEL1_FLAG = "-g1";
    private static final String GENERIC_FLAG = "-generic";
    private static final String GLEVEL_FLAG = "-glevel";
    private static final String HEADER_FLAG = "-headerd";
    private static final String HEADER_SUFFIX_FLAG = "-hsuffix";
    private static final String HELP_FLAG = "-help";
    private static final String NO_CATCH_FLAG = "-nocatch";
    private static final String NO_EXCEPTIONS_FLAG = "-noex";
    private static final String NO_STREAMS_FLAG = "-nostreams";
    private static final String REFLECT_FLAG = "-reflect";
    private static final String RETURN_FLAG = "-return";
    private static final String SERIAL_FLAG = "-serial";
    private static final String SUFFIX_FLAG = "-suffix";
    private static final String SYNC_FLAG = "-sync";
    private static final String VERBOSE_FLAG = "-verbose";
    private static final String VERSION_FLAG = "-version";
    private static final String VVERBOSE_FLAG = "-vverbose";
	private static final String TEMPLATE_SUFFIX_FLAG = "-tsuffix";
	private static final String TEMPLATE_DIR_FLAG = "-tdir";
	private static final String TEMPLATE_NAME_FLAG = "-template";
	private static final String PACKAGE_DIR_FLAG = "-pdir";

	private static final String PACKAGE_LEVEL = "package";

    static
    {
        // Fill in the static languages array.
        _languages = new Language[SmcParser.LANGUAGE_COUNT];
        _languages[TargetLanguage.LANG_NOT_SET.ordinal()] =
            new Language(TargetLanguage.LANG_NOT_SET,
                         "",
                         null,
                         null,
                         null);
        _languages[TargetLanguage.C.ordinal()] =
            new Language(
                TargetLanguage.C,
                "-c",
                "C",
                SmcCGenerator.class,
                SmcHeaderCGenerator.class);
        _languages[TargetLanguage.C_PLUS_PLUS.ordinal()] =
            new Language(
                TargetLanguage.C_PLUS_PLUS,
                "-c++",
                "C++",
                SmcCppGenerator.class,
                SmcHeaderGenerator.class);
        _languages[TargetLanguage.C_SHARP.ordinal()] =
            new Language(
                TargetLanguage.C_SHARP,
                "-csharp",
                "C#",
                SmcCSharpGenerator.class,
                null);
        _languages[TargetLanguage.JAVA.ordinal()] =
            new Language(
                TargetLanguage.JAVA,
                "-java",
                "Java",
                SmcJavaGenerator.class,
                null);
        _languages[TargetLanguage.GRAPH.ordinal()] =
            new Language(
                TargetLanguage.GRAPH,
                "-graph",
                "-graph",
                SmcGraphGenerator.class,
                null);
        _languages[TargetLanguage.GROOVY.ordinal()] =
            new Language(
                TargetLanguage.GROOVY,
                "-groovy",
                "Groovy",
                SmcGroovyGenerator.class,
                null);
        _languages[TargetLanguage.LUA.ordinal()] =
            new Language(
                TargetLanguage.LUA,
                "-lua",
                "Lua",
                SmcLuaGenerator.class,
                null);
        _languages[TargetLanguage.OBJECTIVE_C.ordinal()] =
            new Language(
                TargetLanguage.OBJECTIVE_C,
                "-objc",
                "Objective-C",
                SmcObjCGenerator.class,
                SmcHeaderObjCGenerator.class);
        _languages[TargetLanguage.PERL.ordinal()] =
            new Language(
                TargetLanguage.PERL,
                "-perl",
                "Perl",
                SmcPerlGenerator.class,
                null);
        _languages[TargetLanguage.PHP.ordinal()] =
            new Language(
                TargetLanguage.PERL,
                "-php",
                "PHP",
                SmcPhpGenerator.class,
                null);
        _languages[TargetLanguage.PYTHON.ordinal()] =
            new Language(
                TargetLanguage.PYTHON,
                "-python",
                "Python",
                SmcPythonGenerator.class,
                null);
        _languages[TargetLanguage.RUBY.ordinal()] =
            new Language(
                TargetLanguage.RUBY,
                "-ruby",
                "Ruby",
                SmcRubyGenerator.class,
                null);
        _languages[TargetLanguage.SCALA.ordinal()] =
            new Language(
                TargetLanguage.SCALA,
                "-scala",
                "Scala",
                SmcScalaGenerator.class,
                null);
        _languages[TargetLanguage.TABLE.ordinal()] =
            new Language(
                TargetLanguage.TABLE,
                "-table",
                "-table",
                SmcTableGenerator.class,
                null);
        _languages[TargetLanguage.TCL.ordinal()] =
            new Language(
                TargetLanguage.TCL,
                "-tcl",
                "[incr Tcl]",
                SmcTclGenerator.class,
                null);
        _languages[TargetLanguage.VB.ordinal()] =
            new Language(
                TargetLanguage.VB,
                "-vb",
                "VB.net",
                SmcVBGenerator.class,
                null);
        _languages[TargetLanguage.JS.ordinal()] =
            new Language(
                TargetLanguage.JS,
                "-js",
                "JavaScript",
                SmcJSGenerator.class,
                null);
	    _languages[TargetLanguage.FREEMARKER.ordinal()] =
			    new Language(
					                TargetLanguage.FREEMARKER,
					                "-fm",
					                "FreeMarker",
					                SmcFreeMarkerGenerator.class,
					                null);

	    List<Language> languages = new ArrayList<Language>();

        _optionMap = new HashMap<String, List<Language>>();

        // Languages supporting each option:
        // +    -access:  Java
        // +      -cast:  C++
        // +         -d:  all
        // +         -g:  all
        // +        -g0:  all
        // +        -g1:  all
        // +    -glevel:  graph
        // +    -header:  C, C++, Objective-C
        // +    -hsuffix: C, C++, Objective-C
        // +      -help:  all
        // +   -nocatch:  all
        // +      -noex:  C++
        // + -nostreams:  C++
        // +   -reflect:  C#, Java, TCL, VB, Lua, Perl, PHP,
        //                Python, Ruby, Groovy, Scala
        // +    -return:  all
        // +    -serial:  C#, C++, Java, Tcl, VB, Groovy, Scala
        // +    -suffix:  all
        // +      -sync:  C#, Java, VB, Groovy, Scala
        // +   -verbose:  all
        // +   -version:  all
        // +  -vverbose:  all

        // Set the options supporting all languages first.
        for (TargetLanguage target :
                 EnumSet.allOf(TargetLanguage.class))
        {
            languages.add(_languages[target.ordinal()]);
        }

        _optionMap.put(DIRECTORY_FLAG, languages);
        _optionMap.put(DEBUG_FLAG, languages);
        _optionMap.put(DEBUG_LEVEL0_FLAG, languages);
        _optionMap.put(DEBUG_LEVEL1_FLAG, languages);
        _optionMap.put(HELP_FLAG, languages);
        _optionMap.put(NO_CATCH_FLAG, languages);
        _optionMap.put(RETURN_FLAG, languages);
        _optionMap.put(SUFFIX_FLAG, languages);
        _optionMap.put(VERBOSE_FLAG, languages);
        _optionMap.put(VERSION_FLAG, languages);
        _optionMap.put(VVERBOSE_FLAG, languages);

        // Set the options supported by less than all langugages.
        languages = new ArrayList<Language>();
	    languages.add(_languages[TargetLanguage.C_PLUS_PLUS.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(CAST_FLAG, languages);
        _optionMap.put(NO_EXCEPTIONS_FLAG, languages);
        _optionMap.put(NO_STREAMS_FLAG, languages);

        // The -access option.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.JAVA.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(ACCESS_FLAG, languages);

        // Languages using a header file.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.C_PLUS_PLUS.ordinal()]);
        languages.add(_languages[TargetLanguage.C.ordinal()]);
        languages.add(_languages[TargetLanguage.OBJECTIVE_C.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(HEADER_FLAG, languages);
        _optionMap.put(HEADER_SUFFIX_FLAG, languages);

        // Languages supporting thread synchronization.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(_languages[TargetLanguage.JAVA.ordinal()]);
        languages.add(_languages[TargetLanguage.VB.ordinal()]);
        languages.add(_languages[TargetLanguage.GROOVY.ordinal()]);
        languages.add(_languages[TargetLanguage.SCALA.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(SYNC_FLAG, languages);

        // Languages supporting reflection.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(_languages[TargetLanguage.JAVA.ordinal()]);
        languages.add(_languages[TargetLanguage.VB.ordinal()]);
        languages.add(_languages[TargetLanguage.TCL.ordinal()]);
        languages.add(_languages[TargetLanguage.LUA.ordinal()]);
        languages.add(_languages[TargetLanguage.PERL.ordinal()]);
        languages.add(_languages[TargetLanguage.PHP.ordinal()]);
        languages.add(_languages[TargetLanguage.PYTHON.ordinal()]);
        languages.add(_languages[TargetLanguage.RUBY.ordinal()]);
        languages.add(_languages[TargetLanguage.GROOVY.ordinal()]);
        languages.add(_languages[TargetLanguage.SCALA.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(REFLECT_FLAG, languages);

        // Languages supporting serialization.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(_languages[TargetLanguage.JAVA.ordinal()]);
        languages.add(_languages[TargetLanguage.VB.ordinal()]);
        languages.add(_languages[TargetLanguage.TCL.ordinal()]);
        languages.add(_languages[TargetLanguage.C_PLUS_PLUS.ordinal()]);
        languages.add(_languages[TargetLanguage.GROOVY.ordinal()]);
        languages.add(_languages[TargetLanguage.SCALA.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(SERIAL_FLAG, languages);

        // The -glevel option.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.GRAPH.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(GLEVEL_FLAG, languages);

        // The -generic option.
        languages = new ArrayList<Language>();
        languages.add(_languages[TargetLanguage.C_SHARP.ordinal()]);
        languages.add(_languages[TargetLanguage.JAVA.ordinal()]);
        languages.add(_languages[TargetLanguage.VB.ordinal()]);
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put(GENERIC_FLAG, languages);

	    // The template based options.
	    languages = new ArrayList<Language>();
	    languages.add(_languages[TargetLanguage.FREEMARKER.ordinal()]);
	    _optionMap.put( TEMPLATE_SUFFIX_FLAG, languages );
	    _optionMap.put(TEMPLATE_DIR_FLAG, languages);
	    _optionMap.put(TEMPLATE_NAME_FLAG, languages);
	    _optionMap.put(PACKAGE_DIR_FLAG, languages);
	    // Define the allowed access level keywords for each language
        // which supports the -access option.
        List<String> accessLevels;

        _accessMap = new HashMap<Language, List<String>>();
        accessLevels = new ArrayList<String>();
        accessLevels.add("public");
        accessLevels.add("protected");
        accessLevels.add("package");
        accessLevels.add("private");
        _accessMap.put(
            _languages[TargetLanguage.JAVA.ordinal()], accessLevels);
	    _accessMap.put(
			                  _languages[TargetLanguage.FREEMARKER.ordinal()], accessLevels);
    } // end of static
} // end of class Smc

//
// CHANGE LOG
// $Log: Smc.java,v $
// Revision 1.42  2011/11/20 16:29:53  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.41  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.40  2011/02/14 21:29:56  nitin-nizhawan
// corrected some build errors
//
// Revision 1.38  2010/02/15 18:03:17  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.37  2009/12/17 19:51:43  cwrapp
// Testing complete.
//
// Revision 1.36  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.35  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.34  2009/09/12 21:26:58  kgreg99
// Bug #2857745 resolved. Messages are printed not in case of an error but if there are any.
//
// Revision 1.33  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.32  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.31  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.30  2008/08/15 22:20:40  fperrad
// + move method escape in SmcGraphGenerator.java
//
// Revision 1.29  2008/05/20 18:31:14  cwrapp
// ----------------------------------------------------------------------
//
// Committing release 5.1.0.
//
// Modified Files:
// 	Makefile README.txt smc.mk tar_list.txt bin/Smc.jar
// 	examples/Ant/EX1/build.xml examples/Ant/EX2/build.xml
// 	examples/Ant/EX3/build.xml examples/Ant/EX4/build.xml
// 	examples/Ant/EX5/build.xml examples/Ant/EX6/build.xml
// 	examples/Ant/EX7/build.xml examples/Ant/EX7/src/Telephone.java
// 	examples/Java/EX1/Makefile examples/Java/EX4/Makefile
// 	examples/Java/EX5/Makefile examples/Java/EX6/Makefile
// 	examples/Java/EX7/Makefile examples/Ruby/EX1/Makefile
// 	lib/statemap.jar lib/C++/statemap.h lib/Java/Makefile
// 	lib/Php/statemap.php lib/Scala/Makefile
// 	lib/Scala/statemap.scala net/sf/smc/CODE_README.txt
// 	net/sf/smc/README.txt net/sf/smc/Smc.java
// ----------------------------------------------------------------------
//
// Revision 1.28  2008/04/22 16:05:24  fperrad
// - add PHP language (patch from Toni Arnold)
//
// Revision 1.27  2008/03/21 14:03:16  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.26  2008/02/04 10:32:49  fperrad
// + Added Scala language generation.
//
// Revision 1.25  2008/01/14 19:59:23  cwrapp
// Release 5.0.2 check-in.
//
// Revision 1.24  2008/01/04 20:40:40  cwrapp
// Corrected minor misspellings and incorrect information.
//
// Revision 1.23  2007/08/05 13:53:09  cwrapp
// Version 5.0.1 check-in. See net/sf/smc/CODE_README.txt for more information.
//
// Revision 1.22  2007/07/16 06:28:06  fperrad
// + Added Groovy generator.
//
// Revision 1.21  2007/02/21 13:53:38  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.20  2007/02/13 18:43:19  cwrapp
// Reflect options fix.
//
// Revision 1.19  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.18  2007/01/03 15:37:38  fperrad
// + Added Lua generator.
// + Added -reflect option for Lua, Perl, Python and Ruby code generation
//
// Revision 1.17  2006/09/23 14:28:18  cwrapp
// Final SMC, v. 4.3.3 check-in.
//
// Revision 1.16  2006/09/16 15:04:28  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.15  2006/07/11 18:20:00  cwrapp
// Added -headerd option. Improved command line processing.
//
// Revision 1.14  2006/04/22 12:45:26  cwrapp
// Version 4.3.1
//
// Revision 1.13  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.12  2005/09/19 15:20:03  cwrapp
// Changes in release 4.2.2:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (C#) -csharp not generating finally block closing brace.
//
// Revision 1.11  2005/09/14 01:51:33  cwrapp
// Changes in release 4.2.0:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (Java) -java broken due to an untested minor change.
//
// Revision 1.10  2005/08/26 15:21:34  cwrapp
// Final commit for release 4.2.0. See README.txt for more information.
//
// Revision 1.9  2005/07/07 12:08:44  fperrad
// Added C, Perl & Ruby generators.
//
// Revision 1.8  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the generate Context
// class' accessibility level in Java and C#.
//
// Revision 1.7  2005/06/18 18:28:42  cwrapp
// SMC v. 4.0.1
//
// New Features:
//
// (No new features.)
//
// Bug Fixes:
//
// + (C++) When the .sm is in a subdirectory the forward- or
//   backslashes in the file name are kept in the "#ifndef" in the
//   generated header file. This is syntactically wrong. SMC now
//   replaces the slashes with underscores.
//
// + (Java) If %package is specified in the .sm file, then the
//   generated *Context.java class will have package-level access.
//
// + The Programmer's Manual had incorrect HTML which prevented the
//   pages from rendering correctly on Internet Explorer.
//
// + Rewrote the Programmer's Manual section 1 to make it more
//   useful.
//
// Revision 1.6  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.8  2005/02/21 15:34:25  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.7  2005/02/21 15:09:07  charlesr
// Added -python and -return command line options. Also added an
// undocuments option -vverbose which causes the SmcParser and
// SmcLexer FSMs to enter verbose mode.
//
// Revision 1.6  2005/02/03 16:26:44  charlesr
// SMC now implements the Visitor pattern. The parser returns
// an SmcFSM object which is an SmcElement subclass. SMC then
// creates the appropriate visitor object based on the target
// language and passes the visitor to SmcElement.accept().
// This starts the code generation process.
//
// One minor point: the lexer and parser objects no longer
// write warning and error messages directly to System.err.
// Instead, these messages are collected as SmcMessage objects.
// It is then up to the application calling the parser to
// decide how to display this information. Now the SMC
// application writes these messages to System.err as before.
// This change allows the parser to be used in different
// applications.
//
// Revision 1.5  2004/10/30 16:02:24  charlesr
// Added Graphviz DOT file generation.
// Changed version to 3.2.0.
//
// Revision 1.4  2004/10/08 18:56:07  charlesr
// Update version to 3.1.2.
//
// Revision 1.3  2004/10/02 19:50:24  charlesr
// Updated version string.
//
// Revision 1.2  2004/09/06 16:39:16  charlesr
// Added -verbose and -d options. Added C# support.
//
// Revision 1.1  2004/05/31 13:52:56  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:02:45  charlesr
// Initial revision
//
