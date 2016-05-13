package neurons;

import communication.MyLog;

/**
 * Equivalent to previous "CNeuron".
 * Intermediate neuron between sensors (sensory neurons) and actuators (action neurons).
 * Can be snapped.
 * 
 * @author lana
 *
 */
public class INeuron {
	MyLog mlog = new MyLog("INeuron", true);
	
	
	//int neuron_max_age = Constants.weight_max_age;
	
	/** unique id of the neuron*/
	int id;
	/** ids of the neurons we get input from*/
	//private ArrayList<Integer> input = new ArrayList<Integer>();
	
	
	/**
	 * @param id unique id of the neuron
	 */
	public INeuron(int id){
		this.id = id;
	}
	

}
