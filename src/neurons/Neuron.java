package neurons;

/**
 * A neuron.
 * All neurons extend this class.
 * @author lana
 *
 */
public class Neuron {

	/** unique id of the neuron*/
	int id;
	
	
	
	/**
	 * @param id unique id of the neuron
	 */
	public Neuron(int id){
		this.id = id;
	}
	
	
	/**
	 * 
	 * @return the id of this neuron
	 */
	public int getId(){
		return id;
	}
}
