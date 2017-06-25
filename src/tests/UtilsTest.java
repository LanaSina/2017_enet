package tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.junit.Test;

import communication.Constants;
import communication.MyLog;
import models.Utils;
import neurons.BundleWeight;
import neurons.INeuron;
import neurons.ProbaWeight;

public class UtilsTest {
	MyLog mlog = new MyLog("UtilsTest", true);
	
	@Test
	public void snap_pattern(){
		int id = 0;
		HashMap<Integer, INeuron> neurons = new HashMap<Integer, INeuron> ();
		
		INeuron to = new INeuron(id);
		id++;
		INeuron f1 = new INeuron(id);
		id++;
		INeuron f2 = new INeuron(id);
		id++;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		//create 2 neurons with same bundleweights
		INeuron n = new INeuron(from, to, id);
		id++;
		n.getOutWeights().get(to).setAge(Constants.weight_max_age);
		INeuron n2 = new INeuron(from, to, id);
		n2.getOutWeights().get(to).setAge(Constants.weight_max_age);
		id++;

		neurons.put(n.getId(), n);//3
		neurons.put(n2.getId(), n2);//4
		
		neurons = Utils.snap(neurons);
		assertEquals("size ", 1, neurons.keySet().size());
		INeuron r = neurons.get(3);
		assertEquals("out weights ", true, r.getOutWeights().containsKey(to));
		
		Iterator<Entry<Integer, INeuron>> it = neurons.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, INeuron> entry = it.next();
			INeuron a = entry.getValue();
			
			Vector<BundleWeight> v = a.getDirectInWeights();
			for (Iterator<BundleWeight> iterator = v.iterator(); iterator.hasNext();) {
				BundleWeight bundleWeight = iterator.next();
				Set<INeuron> v2 = bundleWeight.getBundle().keySet();
				for (Iterator<INeuron> iterator2 = v2.iterator(); iterator2.hasNext();) {
					INeuron iNeuron = iterator2.next();
					assertEquals("direct inweights ", true, from.contains(iNeuron));
				}
			}
			
		}
	}
	
	
	@Test
	public void snap_pattern_normal(){
		int id = 0;
		HashMap<Integer, INeuron> neurons = new HashMap<Integer, INeuron> ();
		
		INeuron to = new INeuron(id);
		id++;
		INeuron f1 = new INeuron(id);
		id++;
		INeuron f2 = new INeuron(id);
		id++;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		//
		INeuron n = new INeuron(from, to, id);
		id++;
		n.getOutWeights().get(to).setAge(Constants.weight_max_age);
		
		//n2 has 1 outweight and 1 direct inweight
		INeuron n2 = new INeuron(id);
		id++;
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, n2);
		n2.addOutWeight(to, p);
		n2.getOutWeights().get(to).setAge(Constants.weight_max_age);
		
		INeuron n3 = new INeuron(id);
		id++;
		//add direct in weight
		Vector<INeuron> ve = new Vector<INeuron>();
		ve.addElement(n3);
		BundleWeight b = n2.addDirectInWeight(ve);
		n3.addDirectOutWeight(n2,b);

		neurons.put(n.getId(), n);//3
		neurons.put(n2.getId(), n2);//4
		
		neurons = Utils.snap(neurons);
		assertEquals("size ", 1, neurons.keySet().size());
		INeuron r = neurons.get(3);
		assertEquals("out weights ", true, r.getOutWeights().containsKey(to));
		assertEquals("direct in weights ", true, r.getDirectInWeights().contains(b));
	}
	
	@Test
	public void pattern_activation(){
		int id = 0;
		HashMap<Integer, INeuron> neurons = new HashMap<Integer, INeuron> ();

		INeuron to = new INeuron(id);
		id++;
		INeuron f1 = new INeuron(id);
		id++;
		INeuron f2 = new INeuron(id);
		id++;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		INeuron n = new INeuron(from, to, id);
		id++;
		
		neurons.put(f1.getId(), f1);
		neurons.put(f2.getId(), f2);
		neurons.put(n.getId(), n);
		neurons.put(to.getId(), to);

		//loop
		
		//reset activations of ineurons
		Utils.resetNeuronsActivation(neurons.values());
		//resetDirectOutWeights(allINeurons);
		Utils.resetDirectOutWeights(neurons);
		
		//build input 1
		f1.increaseActivation(1);
		f2.increaseActivation(1);

		//net.updateSNet();
		//update prediction probabilities	
		Utils.ageOutWeights(neurons);
		Utils.increaseInWeights(neurons);
		
		//reset activation of all w
		Utils.resetOutWeights(neurons);
		
		//for ineurons
		activateOutWeights(allINeurons);	
			
		calculateAndPropagateActivation();
		//create new weights based on (+) surprise
		makeWeights();

		
		//input 2
		n.increaseActivation(1);

	}

}
