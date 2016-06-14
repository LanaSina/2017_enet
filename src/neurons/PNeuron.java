package neurons;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

/**
 * Pattern neuron: only activated if all inweights are activated
 * @author lana
 *
 */
public class PNeuron extends INeuron {

	public PNeuron(int id) {
		super(id);
		mlog.setName("Pattern Neuron");
	}
	
	/**
	 * 
	 * @param stm list of pre-neurons
	 * @param n neuron to make the bundleWeigth to
	 */
	public PNeuron(Vector<INeuron> from, INeuron to, int id) {
		super(id);
		mlog.setName("Pattern Neuron");
		
		BundleWeight b = new BundleWeight(from, to);
		directInWeights.put(to, b);
	}


	/**
	 * calculates the probabilistic activation of this neuron
	 * and compares it to its direct activation.
	 * if there is not predicted activation but we are activated, the neuron should be "surprised"*/
	@Override
	public void calculateActivation() {
		mlog.say("Pattern neuron activated");
		super.calculateActivation();
	}

	
	
}
