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
	MyLog mlog = new MyLog("INTest", true);
	
	/*@Test
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
	}*/
	
	@Test
	public void reportDirectInWeights(){
		Vector<INeuron> vn = new Vector<INeuron>();
		
		//from
		INeuron n1 = new INeuron(0);
		double[] p = {1,0,0,0};
		n1.setPosition(p);
		INeuron n2 = new INeuron(1);
		p[0] = 2;
		n2.setPosition(p);
		vn.addElement(n1);
		vn.addElement(n2);
		
		//to
		INeuron n3 = new INeuron(2);
		p[0] = 3;
		n3.setPosition(p);
		INeuron the_pattern = new INeuron(vn,n3,3);
		
		//report to 
		int id = 3;
		INeuron n4 = new INeuron(id);
		p[0] = 4;
		n2.setPosition(p);
		id++;
		the_pattern.reportDirectInWeights(n4);
		n4.recalculatePosition();
		
		mlog.say("size "+ n4.getDirectInWeights().size());
		assertEquals(true,n4.getDirectInWeights().size()==1);
		
		mlog.say("size "+ n1.getDirectOutWeights().size());
		assertEquals(true,n1.getDirectOutWeights().size()==1);
		assertEquals(false,n1.getDirectOutWeights().get(n4)==null);
		
		//activate it
		n1.increaseActivation(1);
		n2.increaseActivation(1);
		//n1.calculateActivation();
		//n2.calculateActivation();
		n1.activateDirectOutWeights();
		n2.activateDirectOutWeights();
		//n4.calculateActivation();
		assertEquals(true,n4.isActivated());
	}
	
	/*@Test
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
	}*/
	
	/*@Test
	public void reportInWeights_no_coactivation(){
		int id = 0;
		INeuron from = new INeuron(id);
		id++;
		INeuron to = new INeuron(id);
		id++;
		
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, from);
		p.setAge(4);//0.5
		from.addOutWeight(to, p);
		
		INeuron n = new INeuron(id);
		id++;
		p = n.addInWeight(Constants.defaultConnection, from);
		p.setAge(4);//0.5
		from.addOutWeight(to, p);
		
		//add coactivation
		ProbaWeight w = new ProbaWeight(Constants.defaultConnection);
		w.setValue(0);
		n.getCoWeights().put(to, w);
		to.getInCoWeights().put(n, w);
		
		w = new ProbaWeight(Constants.defaultConnection);
		w.setValue(0);
		to.getCoWeights().put(n, w);
		n.getInCoWeights().put(to, w);
		
		to.reportInWeights(n);
		w = n.getInWeights().get(from);
		w.setAge(Constants.weight_max_age);
		mlog.say("proba " + w.getProba());
		assertEquals(true, w.getProba()==1);
		assertEquals(true, w.getAge()==Constants.weight_max_age);
	}*/
	
	
	/*@Test
	public void reportInWeights_coactivation(){
		int id = 0;
		INeuron from = new INeuron(id);
		id++;
		INeuron to = new INeuron(id);
		id++;
		
		ProbaWeight p = to.addInWeight(Constants.defaultConnection, from);
		p.setAge(4);//0.5
		from.addOutWeight(to, p);
		
		INeuron n = new INeuron(id);
		id++;
		p = n.addInWeight(Constants.defaultConnection, from);
		p.setAge(4);//0.5
		from.addOutWeight(to, p);
		
		//add coactivation
		ProbaWeight w = new ProbaWeight(Constants.defaultConnection);
		w.setValue(1);
		//n.getCoWeights().put(to, w);
		//to.getInCoWeights().put(n, w);
		
		w = new ProbaWeight(Constants.defaultConnection);
		w.setValue(1);
		to.getCoWeights().put(n, w);
		n.getInCoWeights().put(to, w);
		
		to.reportInWeights(n);
		w = n.getInWeights().get(from);
		w.setAge(Constants.weight_max_age);
		mlog.say("proba " + w.getProba());
		assertEquals(true, w.getProba()==0.5);
		assertEquals(1, n.getInWeights().size());
	}*/
	
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
		assertEquals(false,bundleWeight.bundleIsActivated());
		
		p1.setActivation(1, null);
		assertEquals(true,p1.isActivated());
		assertEquals(true,bundleWeight.bundleIsActivated());
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
