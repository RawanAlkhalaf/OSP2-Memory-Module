/**
 Authors:
 Rawan Alkhalaf - 1605159
 Dana AlAhdal   - 1607540
 */

package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.

   @OSPProject Memory
*/

public class PageTableEntry extends IflPageTableEntry
{
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);

       as its first statement.
       Date of Last Modification: 12/04/2020
       @OSPProject Memory
    */
	
	private long timeStamp; //time stamp the page as it enters the page table
	
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        super(ownerPageTable, pageNumber);
        this.setTimeStamp(HClock.get());

    }


    /**
       This method increases the lock count on the page by one.

	   The method must FIRST increment lockCount, THEN
	   check if the page is valid, and if it is not and no
	   page validation event is present for the page, start page fault
	   by calling PageFaultHandler.handlePageFault().

	   @return SUCCESS or FAILURE
	   FAILURE happens when the pagefault due to locking fails or the
	   that created the IORB thread gets killed.
       Date of Last Modification: 14/04/2020
	   @OSPProject Memory
    */
    public int do_lock(IORB iorb)
    {	
    
    	 ThreadCB th1 = iorb.getThread();
         
         if(!this.isValid()){

           ThreadCB th2 = this.getValidatingThread(); //Thread that caused pagefault

           if(th2 == null){

               //When page in invalid, initiate pagefault
               if(PageFaultHandler.handlePageFault(th1,MemoryLock,this) == FAILURE)
                   return FAILURE;
           }

           else{

               if(th2 != th1){

                   th1.suspend(this); //wait until the page becomes valid
                   if(th1.getStatus() == ThreadKill )
                       return FAILURE;
               }
           }   

         }
        //page is valid
         FrameTableEntry frame = this.getFrame();
         frame.incrementLockCount();

         return SUCCESS;
    	      
    	
	}

    

    /**
       This method decreases the lock count on the page by one.

	   This method must decrement lockCount, but not below zero.
       Date of Last Modification: 14/04/2020
	   @OSPProject Memory
    */
    public void do_unlock()
    {
    	// your code goes here
		if(getFrame().getLockCount()<= 0)
		{
			return;
		}
		else
		{
			getFrame().decrementLockCount();
		}

    }
    
    //Date of Last Modification: 14/04/2020
    public long getTimeStamp(){

        return timeStamp;
    }

    //Date of Last Modification: 14/04/2020
    public void setTimeStamp(long time){

        this.timeStamp = time;
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
