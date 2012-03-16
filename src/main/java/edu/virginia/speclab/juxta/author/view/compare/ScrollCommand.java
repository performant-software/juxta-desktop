/*
 *  Copyright 2002-2010 The Rector and Visitors of the
 *                      University of Virginia. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 

package edu.virginia.speclab.juxta.author.view.compare;



class ScrollCommand
{
    private static final int BOTH_GO = 0;
    private static final int LEFT_STOP = 1;
    private static final int RIGHT_STOP = 2;

    public static final ScrollCommand GO_COMMAND = new ScrollCommand(BOTH_GO);
    public static final ScrollCommand LEFT_STOP_COMMAND = new ScrollCommand(LEFT_STOP);
    public static final ScrollCommand RIGHT_STOP_COMMAND = new ScrollCommand(RIGHT_STOP);

    private int command;
    
    private ScrollCommand( int command )
    {
        this.command = command;            
    }
    
    public int getCommand()
    {
        return command;
    }
    
    public String toString()
    {
        switch(command)
        {
        case BOTH_GO: 
            return "BOTH";
        case LEFT_STOP:
            return "LEFT STOP";
        case RIGHT_STOP:
            return "RIGHT_STOP";
        }
        
        return "INVALID";
    }
    
    public boolean equals( Object o )
    {
        if( o instanceof ScrollCommand )
        {
            ScrollCommand command = (ScrollCommand) o;
            return command.command == this.command;
        }
        
        return false;
    }
}