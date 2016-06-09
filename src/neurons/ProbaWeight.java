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
	/** predicted activation: value is set at t, but integrated to activation at t+1*/
	private int preActivation = 0;
	
	
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
	
	/**
	 * adds 1 to the predicted activation
	 */
	public void addPredictedActivation(){
		preActivation = preActivation+1;
	}
	
	public int getPredictedActivation() {
		return preActivation;
	}
	
	/**
	 * adds the prediction made at t-1 to the activation at t
	 * resets predicted activation to 0
	 */
	public void integratePrediction(){
		activation = activation+preActivation;
		preActivation = 0;
	}

}
