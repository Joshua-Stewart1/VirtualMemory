package VirtualTest;

import java.io.IOException;

import VirtualPD.VirtualMemory;

public class VMTest 
{
	
	public static void main(String args[]) throws IOException
	{
		VirtualMemory myMemory = new VirtualMemory();
		
		myMemory.runVM();
	}
	
}