package VirtualPD;

import java.io.*;
import java.util.*;

public class VirtualMemory 
{
	
	private EntryTLB TLB[];					//List of TLB entries
	private PageTableEntry pageTable[];		//List of Page table entries
	private byte physicalMemory[];			//Representation of physical memory
	private RandomAccessFile backingStore;	//The file holding storage
	
	private ArrayList<Integer> freeFrames;	//List of free frames in memory
	private ArrayList<Integer> freeSlots;	//List of free slots in TLB
	
	private int clock = 0;			//Clock used for timestamping
	private int faultCount = 0;		//The total number of page faults
	private int hitTLB = 0;			//The number of times the TLB is hit
	
	private class EntryTLB
	{
		public int pageNumber = -1;		//The page number in the slot
		public int frameNumber = -1;	//The frame associated with the given page number
		public int timestamp = -1;		//When the slot was allocated
	}
	
	private class PageTableEntry
	{
		public int frameNumber = -1;	//The frame associated with the given page number
		public int timestamp = -1;		//When the frame was allocated
	}
	
	
	public VirtualMemory()
	{
		TLB = new EntryTLB[16];
		pageTable = new PageTableEntry[256];
		physicalMemory = new byte[32768];
		freeFrames = new ArrayList<Integer>();
		freeSlots = new ArrayList<Integer>();
		
		//Fill arrays with default values
		for(int i = 0; i < TLB.length; i++)
		{
			TLB[i] = new EntryTLB();
			freeSlots.add(i);
		}
		for(int i = 0; i < pageTable.length; i++)
		{
			pageTable[i] = new PageTableEntry();
		}
		for(int i = 0; i < 128; i++)
		{
			freeFrames.add(i);
		}
		
	}
	
	//Process the list of instructions
	public void runVM() throws IOException
	{
		
		String s = "";
		
		char c = 'c';
		
		s += c;
		
		
		
		
		backingStore = new RandomAccessFile("BACKING_STORE.bin","r");
		//Open the list of addresses
		File addresses = new File("addresses.txt");
		BufferedReader addressReader = new BufferedReader(new FileReader(addresses));
		String currentLine;
		
		//For each address, get the proper output
		while((currentLine = addressReader.readLine()) != null)
		{
			System.out.println(getOutput(currentLine));
			clock++;
		}
		
		System.out.println("Page Fault Rate: " + faultCount + "/" + clock);
		System.out.println("TLB Hit Rate: " + hitTLB + "/" + clock);
		
		addressReader.close();
	}
	
	//Generate the output for a given input string
	private String getOutput(String input) 
	{
		String output = "Virtual Address: " + input;
		
		//Separate the input data into meaningful parts
		int address = parseAddress(input);
		int pageNumber = address / 256;
		int offset = address % 256;
		
		//Generate the physical address based on the logical address
		int frameNumber = getFrameFromTLB(pageNumber);
		int pysAddress = (frameNumber * 256) + offset;
		
		//Use the physical address to get the byte at that location
		output += " Physical Address: " + pysAddress;
		byte value = physicalMemory[pysAddress];
		output += " Value: " + value;
		return output;
	}
	
	//Get the 16 lower order bits from the provided address
	private int parseAddress(String input)
	{
		int address = Integer.parseInt(input);
		address = address % 65536; //32768
		
		return address;
	}
	
	//Get the frame number from the TLB, or access the page table if not found
	private int getFrameFromTLB(int pageNumber)
	{
		//Check if the page number is in the TLB
		int result = -1;
		for(int i = 0; i < TLB.length && result == -1; i++)
		{
			if(TLB[i].pageNumber == pageNumber)
			{
				result = TLB[i].frameNumber;
			}
		}
		//If not, check the frame table
		if(result < 0)
		{
			result = getFrameFromPageTable(pageNumber);
			
			int freeSlot = getFreeSlot();
			
			TLB[freeSlot].pageNumber = pageNumber;
			TLB[freeSlot].timestamp = clock;
			TLB[freeSlot].frameNumber = result;
		}
		else
		{
			hitTLB++;
		}
		return result;
	}
	
	//Get the frame number from the page table, loading the frame into memory if necessary
	private int getFrameFromPageTable(int pageNumber)
	{
		int result = pageTable[pageNumber].frameNumber;
		//If the frame is not in the page table, load it in from the backing store
		if(result < 0)
		{
			int freeFrame = getFreeFrame();
			loadMemory(pageNumber, freeFrame);
			pageTable[pageNumber].frameNumber = freeFrame;
			pageTable[pageNumber].timestamp = clock;
			faultCount++;
			
		}
		result = pageTable[pageNumber].frameNumber;
		
		return result;
	}
	
	private void loadMemory(int page, int frame)
	{
		try
		{
			backingStore.seek(page * 256);
			backingStore.read(physicalMemory, frame * 256, 256);
		} 
		catch (IOException e){ }
	}
	
	//Get a free frame, replacing when necessary
	private int getFreeFrame()
	{
		if(freeFrames.isEmpty())
		{
			int victim = 10000000;
			int victimPos = 0;
			
			for(int i = 0; i < pageTable.length; i++)
			{
				if(pageTable[i].timestamp < victim && pageTable[i].timestamp >= 0)
				{
					victim = pageTable[i].timestamp;
					victimPos = i;
				}
			}
			freeFrames.add(pageTable[victimPos].frameNumber);
			
			pageTable[victimPos].frameNumber = -1;
			pageTable[victimPos].timestamp = -1;
			
			for(int i = 0; i < TLB.length; i++)
			{
				if(TLB[i].pageNumber == victimPos)
				{
					TLB[i].pageNumber = -1;
					TLB[i].frameNumber = -1;
					TLB[i].timestamp = -1;
				}
			}
		}
		return freeFrames.remove(0);
	}
	
	//Get a free TLB slot, replacing when necessary
	private int getFreeSlot()
	{
		if(freeSlots.isEmpty())
		{
			EntryTLB victim = TLB[0];
			int victimPos = 0;
			
			for(int i = 0; i < TLB.length; i++)
			{
				if(TLB[i].timestamp < victim.timestamp)
				{
					victim = TLB[i];
					victimPos = i;
				}
			}
			freeSlots.add(victimPos);
		}
		return freeSlots.remove(0);
	}
}
