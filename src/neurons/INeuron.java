package neurons;

import java.util.HashMap;

import communication.MyLog;

/**
 * Equivalent to previous "CNeuron".
 * Intermediate neuron between sensors (sensory neurons) and actuators (action neurons).
 * Can be snapped.
 * 
 * @author lana
 *
 */
public class INeuron extends Neuron {
	MyLog mlog = new MyLog("INeuron", true);

	/** fixed input weights from sensors */
	//HashMap<Integer, ProbaWeight> sensorInWeights = new HashMap<Integer, ProbaWeight>();//was realInWeights
	/** input weights */
	HashMap<Integer, ProbaWeight> inWeights = new HashMap<Integer, ProbaWeight>();//was realInWeights
	/** (id of out neuron, weight) probabilistic outweights*/
	HashMap<Integer, ProbaWeight> outWeights = new HashMap<Integer, ProbaWeight>();
	/** activation of this neuron (real or vitual)*/
	double activation;
	
	public INeuron(int id) {
		super(id);
	}


	/**
	 * Adds a neuron as input to this.
	 * 
	 * @param wtype type of the input connection
	 * @param id id of the input neuron
	 * @return existing or newly created connection.
	 */
	public ProbaWeight addInWeight(int wtype, int id) {		
		ProbaWeight p;
		if(inWeights.containsKey(id)){
			p = inWeights.get(id);
		}else{
			p = new ProbaWeight(wtype);
			inWeights.put(id, p);
		}
		return p;
	}


	/**
	 * takes the inweight of a neuron and makes this neuron as input.
	 * @param p weight
	 * @param id id of the output neuron
	 */
	public void addOutWeight(ProbaWeight p, int id) {
		if(outWeights.containsValue(p)){
			mlog.say("addOutWeight: already exists");
		}else{
			outWeights.put(id, p);
		}		
	}
	
	
	/**
	 * sets activation of the neuron to 0
	 * does not change activation of output weights.
	 */
	public void  resetActivation(){
		activation = 0;
	}

}
