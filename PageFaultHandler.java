/**
 Authors:
 Rawan Alkhalaf - 1605159
 Dana AlAhdal   - 1607540
 */

package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault.

        It must check and return if the page is valid,

        It must check if the page is already being brought in by some other
		thread, i.e., if the page has already pagefaulted
		(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.

        If none of the above is true, a new frame must be chosen
        and reserved until the swap in of the requested
        page into this frame is complete.

		Note that you have to make sure that the validating thread of
		a page is set correctly. To this end, you must set the page's
		validating thread using setValidatingThread() when a pagefault
		happens and you must set it back to null when the pagefault is over.

		If no free frame could be found, then a page replacement algorithm
		must be used to select a victim page to be replaced.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated
        from the frame and marked invalid. After the swap-in, the
        frame must be marked clean. The swap-ins and swap-outs
        must be preformed using regular calls to read() and write().

        The student implementation should define additional methods, e.g,
        a method to search for an available frame, and a method to select
        a victim page making its frame available.

		Note: multiple threads might be waiting for completion of the
		page fault. The thread that initiated the pagefault would be
		waiting on the IORBs that are tasked to bring the page in (and
		to free the frame during the swapout). However, while
		pagefault is in progress, other threads might request the same
		page. Those threads won't cause another pagefault, of course,
		but they would enqueue themselves on the page (a page is also
		an Event!), waiting for the completion of the original
		pagefault. It is thus important to call notifyThreads() on the
		page at the end -- regardless of whether the pagefault
		succeeded in bringing the page in or not.

        @param thread		 the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page 		 the memory page

		@return SUCCESS 	 is everything is fine; FAILURE if the thread
		dies while waiting for swap in or swap out or if the page is
		already in memory and no page fault was necessary (well, this
		shouldn't happen, but...). In addition, if there is no frame
		that can be allocated to satisfy the page fault, then it
		should return NotEnoughMemory
     
        Date of Last Modification: 15/04/2020

        @OSPProject Memory
    */
	
	static int pageFault=0;
	
    public static int do_handlePageFault(ThreadCB thread, int referenceType, PageTableEntry page)
    {	
    	
    	if (page.isValid() || page==null) 
			return FAILURE;	
		
		int frameCount=0;
		for(int i=0; i<MMU.getFrameTableSize(); i++){
            if(MMU.getFrame(i).getLockCount() != 0 || MMU.getFrame(i).isReserved() == true)
                frameCount++;          
        }
		
        //No frame is available
        if(MMU.getFrameTableSize() == frameCount)
            return NotEnoughMemory;       

    	pageFault++;
        
        FrameTableEntry frame=null;
        
        SystemEvent systemEvent = new SystemEvent("PageFault");
		thread.suspend(systemEvent);
		
		page.setValidatingThread(thread);
		
		if (numFreeFrames() == 0) 
			//if(userOption.equals(null))
				//frame = SecondChance();
			//else if(userOption.equals("Fifo"))
				frame = Fifo();
			//else if(userOption.equals("SecondChance"))
				//frame=SecondChance();			
		else
			frame = getFreeFrame();
		
		if(!frame.isReserved())
			frame.setReserved(thread.getTask());

			// if the frame contains a dirty page.
		
				PageTableEntry prevPage = frame.getPage();
				
				if(prevPage != null) {
					
					if (frame.isDirty()) {
						
						// swap out
						TaskCB swapTask = frame.getPage().getTask();
						swapTask.getSwapFile().write(prevPage.getID(), prevPage, thread);
						
						if (thread.getStatus() == ThreadKill) {
							
							
							systemEvent.notifyThreads();
							page.notifyThreads();
							ThreadCB.dispatch();
							return FAILURE;
						} 
					
						frame.setDirty(false);
						
						frame.setPage(null);
						frame.setReferenced(false);
						prevPage.setValid(false);
						prevPage.setFrame(null);					
					
					} else { 
						prevPage.getFrame().setReferenced(false);
						prevPage.setValid(false);
						if(prevPage.getFrame().getLockCount() == 0)
							prevPage.setFrame(null);

						}
				
				page.setFrame(frame);
				
				// Swapping in the pages.
				 
				TaskCB task = page.getTask();
				task.getSwapFile().read(page.getID(), page, thread);
				
					if (thread.getStatus() == ThreadKill) {
						if (frame.getPage() != null)
							if (frame.getPage().getTask() == thread.getTask())
								frame.setPage(null);
	
						page.notifyThreads();
						page.setValidatingThread(null);
						page.setFrame(null);
	
						systemEvent.notifyThreads();
						ThreadCB.dispatch();
						return FAILURE;
					}	
				} else { //if frame contains clean page
									
					page.setFrame(frame);				
					
					 // Swapping in the pages.
					 
					TaskCB task = page.getTask();
					task.getSwapFile().read(page.getID(), page, thread);
					
					if (thread.getStatus() == ThreadKill) {
						if (frame.getPage() != null)
							if (frame.getPage().getTask() == thread.getTask())
								frame.setPage(null);
		
						page.notifyThreads();
						page.setValidatingThread(null);
						page.setFrame(null);
		
						systemEvent.notifyThreads();
						ThreadCB.dispatch();
						return FAILURE;
					}
					
				}

		frame.setPage(page);
		page.setValid(true);

		frame.setReferenced(true);
		
		if(referenceType==MemoryWrite)
			frame.setDirty(true);
		
		if (frame.getReserved() == thread.getTask())
			frame.setUnreserved(thread.getTask());

		page.setValidatingThread(null);
		page.notifyThreads();

		systemEvent.notifyThreads();
		ThreadCB.dispatch();
		
		System.out.println(pageFault);
		
		return SUCCESS;
    }
    
    /**
     Returns the current number of free frames. It does not matter where the 
     search in the frame table starts, but this method must not change the value
	 of the reference bits, dirty bits or MMU.Cursor.
     * @return number of free frames
     * Date of Last Modification: 07/04/2020
     */
    
    public static int numFreeFrames() 
    
    {
    	int freeFrames=0;
    	
    	for(int i=0;i<MMU.getFrameTableSize();i++) {
    		//check: 1. no page is associated to frame 
    		//2. frame is not reserved 
    		//3. frame is not locked
    		if(MMU.getFrame(i).getPage()==null && 
    		   !MMU.getFrame(i).isReserved() && 
    		   MMU.getFrame(i).getLockCount()==0)
    			freeFrames++;
    	}
    	
    	return freeFrames;
    	
    }
    
    /**
     * @return the first free frame starting the search from frame[0].
     * Date of Last Modification: 07/04/2020
     */
    
    public static FrameTableEntry getFreeFrame()
    
    {
    	FrameTableEntry frame =null;
    	
    	if(numFreeFrames()!= 0) {
    		
    	for(int i=0;i<MMU.getFrameTableSize();i++) {
    		//check: 1. no page is associated to frame 
    		//2. frame is not reserved 
    		//3. frame is not locked
    		if(MMU.getFrame(i).getPage()==null && 
    		   !MMU.getFrame(i).isReserved() && 
    		   MMU.getFrame(i).getLockCount()==0) {
    			frame=MMU.getFrame(i);
    			break;
    		}   		
    	}
    	return frame;
    	
    	}
    	
    	else
    		return null;
    	
    }
    
    /**
     * Frees frames using the following Second Chance approach and returns one frame. 
     * The search uses the MMU variable MMU.Cursor to specify the starting frame index 
     * of the search.
     * @return frame
     * Date of Last Modification: 19/04/2020
     */
    public static FrameTableEntry SecondChance() 
    
    {
    	FrameTableEntry frame;

    	int dirtyFrameID = 0;
    	int numCleanframes=0;
    	boolean foundDrityFrame=false;
    	
    	//phase 1 
    	
    	for(int i=0; i<MMU.getFrameTableSize();i++) {
    		
    		frame=MMU.getFrame(MMU.Cursor);
    		
    		if(foundDrityFrame==false && frame.getLockCount()<=0 && !frame.isReserved() && frame.isDirty()) {
    			dirtyFrameID=frame.getID();
    			foundDrityFrame=true;
    		}
    		
    		if(frame.isReferenced()) {
    			frame.setReferenced(false);	
    			System.out.println("found refrenced and moved on1");
    			MMU.Cursor  = (MMU.Cursor+1) % MMU.getFrameTableSize();	
    		}
    		
    		
    		frame=MMU.getFrame(MMU.Cursor);
    			
    		//find clean frame
    		if(frame.getPage()!=null && !frame.isReferenced() &&
    		   frame.getLockCount()==0 && !frame.isReserved() && !frame.isDirty()) {
    				
    				PageTableEntry prevPage = frame.getPage();
    				
    				//free the frame
    				frame.setPage(null); 
    	            frame.setReferenced(false);
    	            frame.setDirty(false);
    	            
    	            System.out.println("found clean frame1");
    	            
    	            //update page table
    	            prevPage.setValid(false);
    	            prevPage.setFrame(null);
    	            
    	            //phase 3
    	            numCleanframes = numFreeFrames();
    	            if(numCleanframes==MMU.wantFree)
    	            	return getFreeFrame();    							
    		}

    	}
    
    	
    	//cycle again
    	
    	if(numCleanframes!=MMU.wantFree) {
    		
    		for(int i=0; i<MMU.getFrameTableSize();i++) {
        		
        		frame=MMU.getFrame(MMU.Cursor);
        		
        		if(foundDrityFrame==false && frame.getLockCount()<=0 && !frame.isReserved() && frame.isDirty()) {
        			dirtyFrameID=frame.getID();
        			foundDrityFrame=true;
        		}
        		
        		if(frame.isReferenced()) {
        			frame.setReferenced(false);
        		}
        		
        	
        		
        		//find clean frame
        		if(frame.getPage()!=null && !frame.isReferenced())
        			if(frame.getLockCount()==0 && !frame.isReserved() && !frame.isDirty()) {
        				
        				PageTableEntry prevPage = frame.getPage();
        				
        				//free the frame
        				frame.setPage(null); 
        	            frame.setReferenced(false);
        	            frame.setDirty(false);
        	            
        	            
        	            //update page table for previous page
        	            prevPage.setValid(false);
        	            prevPage.setFrame(null);
        	            
        	            System.out.println("found clean frame2");
        	            
        				numCleanframes = numFreeFrames();
        	            
        	            if(numCleanframes==MMU.wantFree)
        	            	return getFreeFrame(); 
        					
        		}
        		
        		
        		
        		MMU.Cursor  = (MMU.Cursor+1) % MMU.getFrameTableSize(); //update cursor using modulus arithmetic
    		
    		}
    	}
    	
    	//phase 2 
    	
    	if(numCleanframes<MMU.wantFree && !foundDrityFrame)
    		return getFreeFrame();
    	else
    		return MMU.getFrame(dirtyFrameID);
    			
    	
    	
    }
    
//Date of Last Modification: 15/04/2020
    
    public static FrameTableEntry Fifo()
    
    {
    	long maxTimeElapsed = 0;
    	
    	long timeElapsed=0;
    	
        FrameTableEntry replaceFrame = null;
        
        FrameTableEntry frame;
        
        for(int i=0; i<MMU.getFrameTableSize(); i++){ 
        	
        	frame=MMU.getFrame(i);
        	
        	if (frame.getPage()!=null)
        		 timeElapsed = Math.abs(HClock.get()-frame.getPage().getTimeStamp());
        	
        	//System.out.print(frame);
        	//System.out.print(timeElapsed);

            if(timeElapsed > maxTimeElapsed && !frame.isReserved() && frame.getLockCount() == 0){

            	replaceFrame = frame;
                
                maxTimeElapsed = timeElapsed;
                System.out.print(" "+replaceFrame+":");
                System.out.print(maxTimeElapsed);
            }
            
        }
              
        System.out.println("chosen: "+replaceFrame);
        System.out.println("done");
        return replaceFrame;
    }

    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
