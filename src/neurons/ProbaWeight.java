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
	/** value of the activation of this weight at t*/
	private int activation = 0; //could be binary
	/** activation at t-1*/
	private boolean wasActivated = false;
	
	
	/**
	 * Beware: the weight should be aged immediately after being created.
	 * @param type the type of weight (defined in Constants.java)
	 */
	public ProbaWeight(int type){
		switch (type) {
		case Constants.fixedConnection:{
			value = 1;
			break;
		}
		case Constants.defaultConnection:{
			value=2;//they will be aged before the next activation
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
	 * set wasActivated to true or false
	 */
	public void resetActivation() {
		if(activation>0){
			wasActivated = true;
		}else {
			wasActivated = false;
		}
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


	public boolean isActivated() {
		boolean b = false;
		if(activation>0){
			b = true;
		}
		return b;
	}


	public void increaseAge() {
		if(age<Constants.weight_max_age){
			age++;
		}
	}


	public boolean getWasActivated() {
		return wasActivated;
	}
	

}
