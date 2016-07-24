package sensors;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.BitSet;

import communication.MyLog;
import graphics.Surface;

public class ByteSensor {
	/** log */
	MyLog mlog = new MyLog("Eye", true);
	/** graphics*/
	Surface panel;
	/**where to find images files*/
	String textPath;
	
	/** file stream*/
	FileInputStream inputStream;

	
	/**interface for sensory neurons linked to this sensor. [bit value(0,1)][neuron id] */
	int[][] s_neurons; 
	
	public ByteSensor(String textPath, Surface panel){
		//init
		this.textPath = textPath;
		this.panel = panel;
    	
		//sensory neurons
		s_neurons = new int[2][8];
		
		
		//open file
		try {
			inputStream = new FileInputStream(textPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * use to build interface between eye and net.
	 * 
	 * @param nid id of the neuron that will receive input
	 * @param val the bit value this neuron is sensitive to
	 * @param index position of the neuron in the byte
	 */
	public void linkNeuron(int nid, int val, int index) {
		//this sensor position i in this grayscale s links to this neuron		
		s_neurons[val][index] = nid;
	}
	
	
	/**
	 * reads a byte
	 * @param input buffer to write 8 bit values
	 * @return false if end of file reaached
	 */
	public boolean readInput(int[] input){
		Integer i;
		
		try {
			if((i = inputStream.read())!=-1){
				BitSet bitset;
				bitset = BitSet.valueOf(new long[]{i});
				for (int j = 0; j < 8; j++) {
					if(bitset.get(j)){
						input[j] = 1;
					} else{
						input[j] = 0;
					}
				}
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return false;
	}
	
	
	/**
	 * interface for sensory neurons linked to this sensor. 
	 * @return [bit value(0,1)][neuron id]
	 */
	public int[][] getNeuralInterface(){
		return s_neurons;
	}
	
}
