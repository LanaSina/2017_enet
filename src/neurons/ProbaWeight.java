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
	//private int activation = 0;

	
	
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

}
