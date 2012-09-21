package common;

import example_1.Example1;

//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2003 Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// Function
//   Main
//
// Description
//  This routine starts the finite state machine running.
//
// RCS ID
// $Id: checkstring.java,v 1.4 2005/05/28 13:51:23 cwrapp Exp $
//
// CHANGE LOG
// $Log: checkstring.java,v $
// Revision 1.4  2005/05/28 13:51:23  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.1  2004/09/06 15:39:34  charlesr
// Updated for SMC v. 3.1.0.
//
// Revision 1.0  2003/12/14 19:51:08  charlesr
// Initial revision
//

public class checkstring {
    public static void main(String[] args) {
        Example1 appobject = new Example1();
        int retcode = 0;

        if (args.length < 1) {
            System.err.println("No string to check.");
            retcode = 2;
        } else if (args.length > 1) {
            System.err.println("Only one argument is accepted.");
            retcode = 3;
        } else {
            System.out.print("The string \"");
            System.out.print(args[0]);
            System.out.print("\" is ");

            if (appobject.CheckString(args[0]) == false) {
                System.out.println("not acceptable.");
                retcode = 1;
            } else {
                System.out.println("acceptable.");
            }
        }

        System.exit(retcode);
    }
}
