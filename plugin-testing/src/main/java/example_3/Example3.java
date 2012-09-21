package example_3;

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
// Class
//	Example1
//
// Description
//	When a state machine executes an action, it is really calling a
//  member function in the context class.
//
// RCS ID
// $Id: Example1.java,v 1.7 2009/12/17 19:51:43 cwrapp Exp $
//
// CHANGE LOG
// $Log: Example1.java,v $
// Revision 1.7  2009/12/17 19:51:43  cwrapp
// Testing complete.
//
// Revision 1.6  2009/03/27 09:41:46  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.5  2009/03/01 18:20:38  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.4  2005/05/28 13:51:24  cwrapp
// Update Java examples 1 - 7.
//
// Revision 1.0  2003/12/14 19:58:03  charlesr
// Initial revision
//

public class Example3 {
    private Example3Context _fsm;
    private boolean         _is_acceptable;

    public Example3() {
        _fsm = new Example3Context(this);
        _is_acceptable = false;

        // Uncomment to see debug output.
        // _fsm.setDebugFlag(true);

        // Uncomment to output -reflect information.
        // Be sure to uncomment REFLECT and GENERIC macros in
        // Makefile.
        //         System.out.println("States:");
        //         for (AppClassContext.AppClassState state: _fsm.getStates())
        //         {
        //             System.out.print("  ");
        //             System.out.println(state);

        //             System.out.println("    Transitions:");
        //             for (String transition:
        //                      (state.getTransitions()).keySet())
        //             {
        //                 System.out.print("      ");
        //                 System.out.println(transition);
        //             }
        //         }
    }

    public void Acceptable() {
        _is_acceptable = true;
    }

    public boolean CheckString(String string) {
        int i, Length;
        _fsm.enterStartState();

        for (i = 0, Length = string.length(); i < Length; ++i) {
            switch (string.charAt(i)) {
                case '0':
                    _fsm.Zero();
                    break;

                case '1':
                    _fsm.One();
                    break;

                case 'c':
                case 'C':
                    // Uncomment to test serialization.
                    //                     try
                    //                     {
                    //                         final String filename =
                    //                             "./fsm_serial.bin";

                    //                         serialize(
                    //                             new java.io.FileOutputStream(
                    //                                 filename));
                    //                         _fsm =
                    //                             deserialize(
                    //                                 new java.io.FileInputStream(
                    //                                     filename));
                    //                     }
                    //                     catch (Exception jex)
                    //                     {
                    //                         System.err.println(
                    //                             "FSM serialization failure:");
                    //                         jex.printStackTrace();
                    //                     }

                    _fsm.C();
                    break;

                default:
                    _fsm.Unknown();
                    break;
            }
        }

        _fsm.EOS();

        return _is_acceptable;
    }

    public void Unacceptable() {
        _is_acceptable = false;
    }

    // Uncomment to test serialization.
    //     public void serialize(final java.io.OutputStream s)
    //         throws java.io.IOException
    //     {
    //         final java.io.ObjectOutputStream oos =
    //             new java.io.ObjectOutputStream(s);

    //         oos.writeObject(_fsm);

    //         return;
    //     } // end of serialize(OutputStream)

    //     public AppClassContext deserialize(
    //         final java.io.InputStream s)
    //         throws java.io.IOException,
    //                ClassNotFoundException
    //     {
    //         final java.io.ObjectInputStream ois =
    //             new java.io.ObjectInputStream(s);
    //         AppClassContext retval = null;

    //         retval = (AppClassContext) ois.readObject();
    //         retval.setOwner(this);

    //         // Uncomment to see debug output.
    //         // retval.setDebugFlag(true);
    //         // retval.setDebugStream(System.err);

    //         return (retval);
    //     } // end of deserialize(InputStream))
}
