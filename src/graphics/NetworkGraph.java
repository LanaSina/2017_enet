package graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import communication.MyLog;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import neurons.INeuron;
import neurons.ProbaWeight;



/**
 * This class manages visualization of the spiking NN.
 * @author lana
 *
 */
public class NetworkGraph {
	MyLog mlog = new MyLog("graphViz", true);
	String name = "SNet";
	
	/**Graph<V, E> where V is the type of the vertices and E is the type of the edges*/
	//DirectedGraph<NeuronVertex, SynapseEdge> g;
	//DirectedOrderedSparseMultigraph <NeuronVertex, SynapseEdge> g;
	Graph<NeuronVertex, SynapseEdge> g;
    /** visualizer*/
    BasicVisualizationServer<NeuronVertex,SynapseEdge> vv;
    /** number of neurons*/
    int n;
    /** only connections above this weight will be displayed*/
    double minWeight = 0;
    //static NeuronVertex[] vertices;  
    /** id, graphical object */
    static HashMap<Integer, NeuronVertex> vertices = new HashMap<>();

   /**
    * Creates a new instance of NeuronGraph 
    * @param size number of neurons
    * @param weights weights of the network [from][to]
    */
    public NetworkGraph(Vector<INeuron> neurons){//int size, double[][] weights) {
    	n = neurons.size();
    	//vertices = new NeuronVertex[n];
    	//g = new DirectedOrderedSparseMultigraph<NeuronVertex, SynapseEdge>();
        g = new SparseMultigraph<NeuronVertex, SynapseEdge>();
        // Add neurons
    	for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
    		INeuron iNeuron = (INeuron) iterator.next();
    		NeuronVertex nv = new NeuronVertex(iNeuron.getId());
    		vertices.put(iNeuron.getId(), nv);
    		g.addVertex(nv);
    	}
        /*for(int i=0; i<n;i++){
        	NeuronVertex nv = new NeuronVertex(i);
        	vertices[i] = nv;
        	g.addVertex(nv);
        }*/
        
        //add edges
        for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron iNeuron = (INeuron) iterator.next();
			//iterate over outweights
			HashMap<INeuron, ProbaWeight> weights = iNeuron.getOutWeights();
			for (Iterator<Entry<INeuron, ProbaWeight>> iterator2 = weights.entrySet().iterator(); iterator2.hasNext();) {
				Entry<INeuron, ProbaWeight> pair = iterator2.next();
				ProbaWeight p = pair.getValue();
				INeuron out = pair.getKey();
				if(p.getProba()>minWeight){
					String label = "w_"+iNeuron.getId()+","+out.getId();
        			SynapseEdge se = new SynapseEdge(label, p.getProba());
        			g.addEdge(se, vertices.get(iNeuron.getId()), vertices.get(out.getId()));
				}
			}
		}
        
        
        /*for(int i=0; i<n;i++){
        	for(int j=0; j<n; j++){
        		if((i!=j) && (weights[i][j]>=minWeight)){
        			String label = "w_"+i+","+j;
        			SynapseEdge se = new SynapseEdge(label, weights[i][j]);
        			g.addEdge(se, vertices[i],vertices[j]);
        		}        			
        	}        	
        }  */
        
        
    }
    
    public void setName(String n){
    	this.name = n;
    }
    
    /**
     * will show if neurons are activatedor not
     * @param neurons array of IzhNeurons
     */
   /* public void updateNeurons(INeuron[] neurons){
    	for(int i=0; i<n;i++){
    		vertices[i].setSpiking(false);
    		if(neurons[i].isActivated())
    			vertices[i].setSpiking(true);
    	}
    	
    	//repaint out of main thread
    	Runnable code = new Runnable() {
        	public void run() {
        		vv.repaint();
        	}
        };

        (new Thread(code)).start();
    }*/
    
    /**
     * 
     * @param weights double array of synaptic weights
     */
    public void update(Vector<INeuron> neurons){//double[][] weights){
    	
    	for (Iterator<INeuron> iterator = neurons.iterator(); iterator.hasNext();) {
			INeuron iNeuron = iterator.next();
			//iterate over outweights
			HashMap<INeuron, ProbaWeight> weights = iNeuron.getOutWeights();
			for (Iterator<Entry<INeuron, ProbaWeight>> iterator2 = weights.entrySet().iterator(); iterator2.hasNext();) {
				Entry<INeuron, ProbaWeight> pair = iterator2.next();
				ProbaWeight p = pair.getValue();
    			double w = p.getProba();
				INeuron out = pair.getKey();
				SynapseEdge e = g.findEdge(vertices.get(iNeuron.getId()),vertices.get(out.getId()));
        		if(e!=null){  
	        		if(Math.abs(e.weight-w)>0.2){
	        			g.removeEdge(e);
	        			if(w>=minWeight){
	    					String label = "w_"+iNeuron.getId()+","+out.getId();
	            			SynapseEdge se = new SynapseEdge(label, w);
	            			g.addEdge(se, vertices.get(iNeuron.getId()),vertices.get(out.getId()));
	            		}   
	        		}    
	        	} else{
	        		//TODO make this a function
	        		String label = "w_"+iNeuron.getId()+","+out.getId();
        			SynapseEdge se = new SynapseEdge(label, w);
        			g.addEdge(se, vertices.get(iNeuron.getId()),vertices.get(out.getId()));
	        	}
			}
		}
        
        /*for(int i=0; i<n;i++){
        	for(int j=0; j<n; j++){ 
        		double w = weights[i][j];
        		SynapseEdge e = g.findEdge(vertices[i],vertices[j]);
        		if(e!=null){    				        		
	        		if(Math.abs(e.weight-w)>0.5){
	        			g.removeEdge(e);
	        			if(w>=minWeight){
	            			String label = "w_"+i+","+j;
	            			SynapseEdge se = new SynapseEdge(label, w);
	            			g.addEdge(se, vertices[i],vertices[j]);
	            		}   
	        		}    
	        	} else{
	        		if(w>=minWeight){
            			String label = "w_"+i+","+j;
            			SynapseEdge se = new SynapseEdge(label, w);
            			g.addEdge(se, vertices[i],vertices[j]);
            		}   
	        	}
        	}        	
        }*/
    	
        Runnable code = new Runnable() {
        	public void run() {
        		vv.repaint();  
        	}
        };

        (new Thread(code)).start();	
    }
    
    
    
    public void show() {

        // The Layout<V, E> is parameterized by the vertex and edge types
        
    	//Layout<NeuronVertex, String> layout = new FRLayout(g);//CircleLayout(g);
    	Layout<NeuronVertex, SynapseEdge>  layout = new ISOMLayout<NeuronVertex, SynapseEdge>(g);
    	
    	
        layout.setSize(new Dimension(700,700)); // sets the initial size of the layout space
        // The BasicVisualizationServer<V,E> is parameterized by the vertex and edge types
        vv = new BasicVisualizationServer<NeuronVertex,SynapseEdge>(layout);
        vv.setPreferredSize(new Dimension(800,800)); //Sets the viewing area size
        vv.getRenderer().setVertexRenderer(new MyRenderer());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        vv.getRenderContext().setVertexLabelTransformer(new Transformer<NeuronVertex, String>() {
            public String transform(NeuronVertex nv) {
                return (nv.toString());
            }
        });//*/
        
        
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(500, 150);
        frame.getContentPane().add(vv); 
        frame.pack();
        frame.setVisible(true);       
    }
    
    /**
     * Custom node class to display neurons
     * @author lana
     *
     */
    class NeuronVertex {
    	 int id; 
    	 private boolean isSpiking = false;
    	// private int type = Constants.HiddenCluster;
    	 
    	 public NeuronVertex(int id) {
    		 this.id = id;
    	 }
    	 
    	/* public NeuronVertex(int id, int type) {
    		 this.id = id;
    		 this.type = type;
    	 }*/
    	 
    	 public void setSpiking(boolean b){
    		 isSpiking = b;
    	 }
    	 
    	/* public void setType(int t){
    		 type = t;
    	 }*/
    	 
    	 public String toString() { 
    		 return ""+id; 
    	 }
    }
    
    /**
     * Custom edge class
     * @author lana 
     */

    class SynapseEdge{
    	double weight = 0;
    	String label;
    	
    	public SynapseEdge(String label, double weight){
    		this.label = label;
    		this.weight = weight;
    	}
    	
    	public String toString() { 
   		 return label; 
   	 	}
    }
    /**
     * Custom vertex visualization.
     * @author lana
     *
     */
    static class MyRenderer implements Vertex<NeuronVertex, SynapseEdge> {
        /*public void paintVertex(RenderContext<String, String> rc, Layout<String, String> layout, String vertex) {
          GraphicsDecorator graphicsContext = rc.getGraphicsContext();
          Point2D center = layout.transform(vertex);
          Shape shape = null;
          Color color = null;
          //if(vertex.equals("Square")) {
            shape = new Ellipse2D.Double(center.getX()-10, center.getY()-10, 20, 20);
            color = Color.BLUE;
          //}
          graphicsContext.setPaint(color);
          graphicsContext.fill(shape);
        }*/

		public void paintVertex(RenderContext<NeuronVertex, SynapseEdge> rc, Layout<NeuronVertex, SynapseEdge> layout, NeuronVertex vertex) {
			GraphicsDecorator graphicsContext = rc.getGraphicsContext();
	          Point2D center = layout.transform(vertex);
	          Shape shape = null;
	          Color color = null;
	          
	          shape = new Ellipse2D.Double(center.getX()-10, center.getY()-10, 20, 20);
	          NeuronVertex nv = vertices.get(vertex.id);
	          if(nv.isSpiking) {	    
	        	  color = Color.cyan;	         
	          } else{
	        	  color = Color.lightGray;  	        	  
	          }
	          graphicsContext.setPaint(Color.black);
	          graphicsContext.draw(shape);
	          graphicsContext.setPaint(color);
	          graphicsContext.fill(shape);
		}
      }
}
