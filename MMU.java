package osp.Memory;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
 Authors:
 Rawan Alkhalaf - 1605159
 Dana AlAhdal   - 1607540
 */

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.
 
    Date of Last Modification: 10/04/2020

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /**
        This method is called once before the simulation starts.
		Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
	public static int Cursor;
	public static int wantFree;
	
    public static void init()
    {
        Cursor=0;
        wantFree=1;
        
 	   for(int i = 0; i < MMU.getFrameTableSize(); i++)
 	   {
       	setFrame(i, new FrameTableEntry(i));
       }

    }

    /**
       This method handles memory references. The method must
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault
       by making an interrupt if the page is invalid, finally,
       if the page is still valid, i.e., not swapped out by another
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue,
       and it is possible that some other thread will take away the frame.)

       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.
     
       Date of Last Modification: 10/04/2020

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
    	// find the page to which the reference was made
    			//pageNo = address/2^offset
    			int pageNumber = memoryAddress / (int)Math.pow(2.0, MMU.getVirtualAddressBits() - MMU.getPageAddressBits());
    			//get pageTableEntry
    			PageTableEntry page = getPTBR().pages[pageNumber];
    			
    			if(page.isValid())
    			{
    				
    				page.getFrame().setReferenced(true);
    				
    				if(referenceType == GlobalVariables.MemoryWrite)
    				{
    					page.getFrame().setDirty(true);
    				}
    				return page;
    			}
    			else
    			{
    				if(page.getValidatingThread() == null)
    				{
    					//if the pageFault occurs due to the original thread, set pagefault as true and handle the pagefault
    					
    					InterruptVector.setInterruptType(referenceType);
    					InterruptVector.setPage(page);
    					InterruptVector.setThread(thread);
    					
    					CPU.interrupt(PageFault);
    					
    					if(thread.getStatus() == GlobalVariables.ThreadKill)
    					{
    						return page;
    					}
    				}
    				else
    				{
    					//if pagefault occurs due to some other thread, suspend this thread and wait
    					
    					thread.suspend(page);
    					
    					if(thread.getStatus() == GlobalVariables.ThreadKill)
    					{
    						return page;
    					}
    				}
    			}
    			
    			page.getFrame().setReferenced(true);
    			
    			if(referenceType == GlobalVariables.MemoryWrite)
    			{
    				//set the dirty bit as true if reference type was write
    				page.getFrame().setDirty(true);
    			}
    			
    			return page;
    }



    /** Called by OSP after printing an error message. The student can
		insert code here to print various tables and data structures
		in their state just after the error happened.  The body can be
		left empty, if this feature is not used.

		@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here (if needed)

    }

    /** Called by OSP after printing a warning message. The student
		can insert code here to print various tables and data
		structures in their state just after the warning happened.
		The body can be left empty, if this feature is not used.

      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here (if needed)

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
