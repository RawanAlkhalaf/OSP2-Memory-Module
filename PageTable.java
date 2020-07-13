/**
 Authors:
 Rawan Alkhalaf - 1605159
 Dana AlAhdal   - 1607540
 */

package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /**
	   The page table constructor. Must call

	       super(ownerTask)

	   as its first statement. Then it must figure out
	   what should be the size of a page table, and then
	   create the page table, populating it with items of
	   type, PageTableEntry.
        Date of Last Modification: 10/04/2020
	   @OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        // your code goes here
        super(ownerTask);
      	/*
    	page table is an array of size equal to the maximum number of pages allowed
    	calculate maximal number of pages allowed
    	*/
        
      	int MaxnumberOfPages = (int)Math.pow(2, MMU.getPageAddressBits());
        	
        	pages = new PageTableEntry[MaxnumberOfPages];
        	
        	
    	/* initialize each page with page table entry */
        for(int i = 0; i < MaxnumberOfPages; i++)
        	{
        		pages[i] = new PageTableEntry(this, i);
        	}

        
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.
       Date of Last Modification: 10/04/2020

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
    	TaskCB pagetableTask = getTask();
        for(int i = 0; i < MMU.getFrameTableSize(); i++)
        {
        	FrameTableEntry frame = MMU.getFrame(i);
        	PageTableEntry page = frame.getPage();
        	if(page != null && page.getTask() == pagetableTask)
        	{
        		frame.setPage(null);
                frame.setDirty(false);
                frame.setReferenced(false);
                if(frame.getReserved() == pagetableTask)
        			frame.setUnreserved(pagetableTask);
        	}
        }

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
