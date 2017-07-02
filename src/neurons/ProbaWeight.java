package neurons;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import communication.Constants;
import communication.MyLog;

/**
 * Probabilistic connections between sensors, neurons or actuators
 * @author lana
 *
 */
public class ProbaWeight {
	/** log */
	MyLog mlog = new MyLog("PWeight",true);
	
	/** age of this weight*/
	int age = 1;
	/** value of this weight*/
	protected int value = 0;
	/** value of the activation of this weight at t*/
	private int activation = 0; //could be binary
	/** bad */
	
	
	/**
	 * @param type the type of weight (defined in Constants.java)
	 */
	public ProbaWeight(int type){
		switch (type) {
		case Constants.fixedConnection:{
			value = 1;
			age = Constants.weight_max_age;//to prevent learning
			break;
		}
		case Constants.defaultConnection:{
			value = 2;
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
		activation = 0;		
	}

	/**
	 * @param a activation value
	 * @param n ignore if not bundle weight
	 */
	public void setActivation(int a, INeuron n) {
		activation = a;
	}

	public void setValue(int v) {
		value = v;
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
	
	public void setAge(int a) {
		age = a;
	}

	public int getValue() {
		return value;
	}


	public int getAge() {
		return age;
	}


	public boolean canLearn() {
		if(age>=Constants.weight_max_age){
			return false;
		}else {
			return true;
		}
	}


	public boolean sameBundle(Set<INeuron> neurons) {
		return false;
	}

	//TODO
	public void muteInputNeurons() {
		// TODO Auto-generated method stub
	}
	
	public HashMap<INeuron, ProbaWeight> getBundle() {
		return null;
	}

}
