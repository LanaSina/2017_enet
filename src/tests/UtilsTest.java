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
import communication.Utils;
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
		to.justSnapped = false;
		f1.justSnapped = false;
		f2.justSnapped = false;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		//create 2 neurons with same bundleweights
		INeuron n = new INeuron(from, to, id);
		id++;
		n.justSnapped = false;
		n.getOutWeights().get(to).setAge(Constants.weight_max_age);
		INeuron n2 = new INeuron(from, to, id);
		n2.getOutWeights().get(to).setAge(Constants.weight_max_age);
		id++;
		n2.justSnapped = false;


		neurons.put(n.getId(), n);//3
		neurons.put(n2.getId(), n2);//4
		
		neurons = Utils.snap(neurons);
		assertEquals("size ", 1, neurons.keySet().size());
		INeuron r = neurons.values().iterator().next();
		assertEquals(false, r==null);
		assertEquals("out weights ", true, r.getOutWeights().containsKey(to));
		assertEquals(1, r.getOutWeights().size());
		
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
		
		to.justSnapped = false;
		f1.justSnapped = false;
		f2.justSnapped = false;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		//n has 2 direct inweights from [f1,f2], one weight to "to"
		INeuron n = new INeuron(from, to, id);
		id++;
		n.getOutWeights().get(to).setAge(Constants.weight_max_age);
		n.justSnapped = false;
		
		//n2 has one weight to "to", one direct weight from n3
		INeuron n2 = new INeuron(id);
		id++;
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, n2);
		n2.addOutWeight(to, p);
		n2.getOutWeights().get(to).setAge(Constants.weight_max_age);
		n2.justSnapped = false;
		
		INeuron n3 = new INeuron(id);
		id++;
		n3.justSnapped = false;
		//add direct in weight
		Vector<INeuron> ve = new Vector<INeuron>();
		ve.addElement(n3);
		BundleWeight b = n2.addDirectInWeight(ve);
		n3.addDirectOutWeight(n2,b);

		neurons.put(n.getId(), n);//3
		neurons.put(n2.getId(), n2);//4
		
		neurons = Utils.snap(neurons);
		assertEquals("size ", 1, neurons.keySet().size());
		INeuron r = neurons.values().iterator().next();
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

		f1.activateDirectOutWeights();
		f2.activateDirectOutWeights();
		assertEquals("muted ", true, f1.isMute());
		assertEquals("muted ", true, f2.isMute());

		//net.updateSNet();
		//update prediction probabilities	
		Utils.ageOutWeights(neurons);
		Utils.increaseInWeights(neurons);
		
		//reset activation of all w
		Utils.resetOutWeights(neurons);
		
		//for ineurons
		Utils.activateOutWeights(neurons);	
		Utils.calculateAndPropagateActivation(neurons);
		
		assertEquals("out weight ", 2, to.getInWeights().get(n).getAge());
		mlog.say(" to.getInWeights().get(n).getProba()" +  to.getInWeights().get(n).getProba());
		assertEquals("proba ", true, 1 == to.getInWeights().get(n).getProba());

		//reset activations of ineurons
		Utils.resetNeuronsActivation(neurons.values());
		//resetDirectOutWeights(allINeurons);
		Utils.resetDirectOutWeights(neurons);
		
		//build input 1
		f1.increaseActivation(1);
		f2.increaseActivation(1);

		f1.activateDirectOutWeights();
		f2.activateDirectOutWeights();

		//net.updateSNet();
		//update prediction probabilities	
		Utils.ageOutWeights(neurons);
		Utils.increaseInWeights(neurons);
		
		//reset activation of all w
		Utils.resetOutWeights(neurons);
		
		//for ineurons
		Utils.activateOutWeights(neurons);	
		Utils.calculateAndPropagateActivation(neurons);
		
		assertEquals("out weight ", 3, to.getInWeights().get(n).getAge());
		mlog.say(" to.getInWeights().get(n).getProba()" +  to.getInWeights().get(n).getProba());
		assertEquals("proba ", true, 0.7 > to.getInWeights().get(n).getProba());

		
		//create new weights based on (+) surprise
		//makeWeights();

		//
		
		//input 2
		//n.increaseActivation(1);

	}
	
	@Test
	public void pattern_exists(){
		int id = 0;
		HashMap<Integer, INeuron> neurons = new HashMap<Integer, INeuron> ();

		INeuron to = new INeuron(id);
		double[] p = {0,0,0,0};
		to.setPosition(p);
		id++;
		INeuron f1 = new INeuron(id);
		double[] p1 = {0,1,0,0};
		f1.setPosition(p1);
		id++;
		INeuron f2 = new INeuron(id);
		double[] p2 = {0,2,0,0};
		f1.setPosition(p2);
		id++;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		INeuron n = new INeuron(from, to, id);
		double[] pp = n.getPosition();
		mlog.say("position " + pp[0] + " " + pp[1] + " " + pp[2] + " " + pp[3]);
		id++;
		
		neurons.put(f1.getId(), f1);
		neurons.put(f2.getId(), f2);
		neurons.put(n.getId(), n);
		neurons.put(to.getId(), to);

		//assertEquals("exists ", true, Utils.patternExists(from, to, neurons.values()));
		assertEquals("exists ", 0, Utils.patternExists3D(from, to).size());
				
		Vector<INeuron> a = new Vector<INeuron>();
		a.addElement(n);
		//assertEquals("exists ", true, Utils.patternExists(a, to, neurons.values()));
		assertEquals("exists ", 0, Utils.patternExists3D(a, to).size());

		//pattern from self to self
		//assertEquals("exists ", true, Utils.patternExists(a, n, neurons.values()));
		assertEquals("exists ", 0, Utils.patternExists3D(a, n).size());

	}
	
	
	@Test
	public void pattern_not_exist(){
		int id = 0;
		HashMap<Integer, INeuron> neurons = new HashMap<Integer, INeuron> ();

		INeuron to = new INeuron(id);
		double[] p0 = {0,0,0,0};
		to.setPosition(p0);
		id++;
		INeuron f1 = new INeuron(id);
		double[] p1 = {0,1,0,0};
		f1.setPosition(p1);
		id++;
		INeuron f2 = new INeuron(id);
		double[] p2 = {0,2,0,0};
		f1.setPosition(p2);
		id++;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, f1);
		f1.addOutWeight(to, p);
		p = to.addInWeight(Constants.defaultConnection, f2);
		f2.addOutWeight(to, p);

		
		neurons.put(f1.getId(), f1);
		neurons.put(f2.getId(), f2);
		
		for (Iterator<INeuron> iterator = neurons.values().iterator(); iterator.hasNext();) {
			INeuron n = iterator.next();
			mlog.say("test : "+ n.getId());
		}
		
		//assertEquals("create ", false, Utils.patternExists(from, to, neurons.values()));
		assertEquals("create ", 2, Utils.patternExists3D(from, to).size());

		INeuron i1 = new INeuron(id);
		i1.setPosition(p1);
		id++;
		INeuron i2 = new INeuron(id);
		i2.setPosition(p2);
		id++;
		
		Vector<INeuron> v = new Vector<INeuron>();
		v.addElement(i1);
		BundleWeight b = f1.addDirectInWeight(v);
		i1.addDirectOutWeight(f1, b);
		v = new Vector<INeuron>();
		v.addElement(i2);
		b = f2.addDirectInWeight(v);
		i2.addDirectOutWeight(f2, b);
		
		//assertEquals("create ", false, Utils.patternExists(from, to, neurons.values()));
		assertEquals("exists ", false, 0==Utils.patternExists3D(from, to).size());
	}

}
