package tests;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.Test;

import neurons.INeuron;

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

}
