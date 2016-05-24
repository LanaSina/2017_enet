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
	/** activation of this neuron (real or vitual)*/
	private double activation;

	
	/**
	 * @param id unique id of the neuron
	 */
	public MotorNeuron(int i){
		super(i);
	}
	
	/**
	 * sets activation of the neuron to 0
	 * does not change activation of output weights.
	 */
	public void  resetActivation(){//all these functions could be in parent class
		setActivation(0);
	}
	
	/**
	 * increases neuron activation
	 * should not be used outside of direct connection to sensors
	 * @param i value to add
	 */
	public void increaseActivation(int i) {
		setActivation(getActivation()+i);
	}

	public double getActivation() {
		return activation;
	}

	public void setActivation(double activation) {
		this.activation = activation;
	}
}
