package neurons;

import communication.MyLog;

/**
 * Neurons that direct actions.
 * @author lana
 *
 */
public class MotorNeuron extends Neuron {	
	/** log */
	MyLog mlog = new MyLog("CMNeuron", true);

	
	/**
	 * @param id unique id of the neuron
	 */
	public MotorNeuron(int i){
		super(i);
	}
}
