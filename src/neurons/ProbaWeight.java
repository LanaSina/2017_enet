package neurons;

import communication.Constants;
import communication.MyLog;

/**
 * Probabilistic connections between sensors, neurons or actuators
 * @author lana
 *
 */
public class ProbaWeight {
	/** log */
	MyLog mlog = new MyLog("PWeight", true);
	
	/** age of this weight*/
	int age = 1;
	/** value of this weight*/
	private int value = 0;
	/** depends on age of this weight */
	boolean canLearn = true;
	/** value of the activation of this weight*/
	private int activation = 0; //could be binary

	
	
	/**
	 * @param type the type of weight (defined in Constants.java)
	 */
	public ProbaWeight(int type){
		switch (type) {
		case Constants.fixedConnection:{
			value = 1;//I think we never make fixed connections
			break;
		}
		default:
			break;
		}
	}


	/**
	 * 
	 * @return the current activation of this weight
	 */
	public int getActivation() {
		return activation;
	}

	/**
	 * adds 1 to current value.
	 */
	public void addValue() {
		value++;		
	}


	/**
	 * resets the activation of this weight to 0
	 */
	public void resetActivation() {
		activation = 0;		
	}

	/**
	 * @param a activation value
	 */
	public void setActivation(int a) {
		activation = a;
	}


	/**
	 * @return the probabilistic value of this weight.
	 */
	public double getProba() {
		double p = value;
		return p/age;
	}

}
