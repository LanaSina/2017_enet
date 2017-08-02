package tests;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.junit.Test;

import communication.Constants;
import communication.MyLog;
import neurons.BundleWeight;
import neurons.INeuron;
import neurons.ProbaWeight;

public class INeuronTest {
	
	@Test
	public void sameDirectInWeight(){
		int id = 0;
		INeuron to = new INeuron(id);
		id++;
		INeuron f1 = new INeuron(id);
		id++;
		INeuron f2 = new INeuron(id);
		id++;
		
		Vector<INeuron> from = new Vector<INeuron>();
		from.addElement(f1);
		from.addElement(f2);
		
		//create 2 neurons with bundleweights
		INeuron n = new INeuron(from, to, id);
		id++;
		
		INeuron n2 = new INeuron(from, to, id);
		id++;
		
		assertEquals(" ", false, n.addDirectInWeight(n.getDirectInWeights().get(0)));
		assertEquals(" ", false, n.addDirectInWeight(n2.getDirectInWeights().get(0)));
	}
	
	@Test
	public void reportInWeights(){
		int id = 0;
		INeuron from = new INeuron(id);
		id++;
		INeuron to = new INeuron(id);
		id++;
		
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, from);
		from.addOutWeight(to, p);
		
		INeuron n = new INeuron(id);
		id++;
		
		to.reportInWeights(n);
		assertEquals("in", true, n.getInWeights().containsKey(from));
		assertEquals("out", true, from.getOutWeights().containsKey(n));
	}
	
	@Test
	public void createPattern(){
		Vector<INeuron> vn = new Vector<INeuron>();
		
		//from
		INeuron n1 = new INeuron(0);
		INeuron n2 = new INeuron(1);
		vn.addElement(n1);
		vn.addElement(n2);
		
		//to
		INeuron n3 = new INeuron(2);

		
		INeuron the_pattern = new INeuron(vn,n3,3);
		ProbaWeight weight = the_pattern.getOutWeights().get(n3);
		//assertEquals(1, weight.getAge());
		assertEquals(n3.getInWeights().get(the_pattern), weight);
	}
	
	@Test
	public void bundleUpdate(){
		//from
		Vector<INeuron> vn = new Vector<INeuron>();
		INeuron n1 = new INeuron(0);
		INeuron n2 = new INeuron(1);
		vn.addElement(n1);
		vn.addElement(n2);
		
		//to
		INeuron n3 = new INeuron(2);
		
		INeuron the_pattern = new INeuron(vn,n3,3);
		
		Vector<BundleWeight> pr = the_pattern.getDirectInWeights();
		BundleWeight bundleWeight = pr.iterator().next();
		vn = new Vector<INeuron>();
		vn.addElement(n1);
		bundleWeight.decreaseAllBut(vn);
		
		ProbaWeight p2 = bundleWeight.getStrand(n2);
		assertEquals(true,p2.getProba()==0.5);
		
		ProbaWeight p1 = bundleWeight.getStrand(n1);
		assertEquals(true,p1.getProba()==1);
		
		p2.setAge(3);
		p2.setActivation(1, null);
		assertEquals(true,p2.isActivated());
		assertEquals(false,bundleWeight.isActivated());
		
		p1.setActivation(1, null);
		assertEquals(true,p1.isActivated());
		assertEquals(true,bundleWeight.isActivated());
	}
	
	
	/*@Test
	public void reportInWeights_pattern(){
		int id = 0;
		INeuron from = new INeuron(id);
		id++;
		INeuron to = new INeuron(id);
		id++;
		
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, from);
		from.addOutWeight(to, p);
		
		INeuron n = new INeuron(id);
		id++;
		
		to.reportInWeights(n);
		assertEquals("in", true, n.getInWeights().containsKey(from));
		assertEquals("out", true, from.getOutWeights().containsKey(n));
	}*/

}
