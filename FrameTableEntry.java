package osp.Memory;

/**
 Authors:
 Rawan Alkhalaf - 1605159
 Dana AlAhdal   - 1607540
 */

/**
 The FrameTableEntry class contains information about a specific page
 frame of memory.
 
 @OSPProject Memory
 */
import osp.Tasks.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.Hardware.HClock;
import osp.IFLModules.IflFrameTableEntry;

public class FrameTableEntry extends IflFrameTableEntry
{
    /**
     The frame constructor. Must have
     
     super(frameID)
     
     as its first statement.
     
     Date of Last Modification: 17/04/2020
     
     @OSPProject Memory
     */
  
    
    public FrameTableEntry(int frameID)
    {
        super(frameID);
    }
    

    
    /*
     Feel free to add methods/fields to improve the readability of your code
     */
    
}

/*
 Feel free to add local classes to improve the readability of your code
 */

