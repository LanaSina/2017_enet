package sensors;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import communication.Constants;
import communication.MyLog;
import graphics.Surface;
import neurons.INeuron;

/**
 * each sensor is just a list of values that are activated or not.
 * Sensor should be an interface.
 * 
 * @author lana
 *
 */
public class Eye {
	/** log */
	MyLog mlog = new MyLog("Eye", true);
	/** graphics*/
	Surface panel;
	
	/** total number of neurons sensitive to one grayscale value*/
	int n;
	/**where to find images files*/
	String imagesPath;
	/** size of current image we're looking at*/
	int im_h,im_w;
	/** visual field size */
	int vf_h = 50, vf_w = 50;
	/** size of focused area */
	int ef_s = 20;
	/** matrix of exact balck and white values for the image */
	//double[][] bw;//black and white
	/** what the net sees */
	double[][] visual_field;//focus
	/** resolution of focused area = px/side of square */ 
	int eres_f = 2;
	/** resolution of non focused area = px/side of square */ 
	int eres_nf = 5;
		
	/** sensitivity resolution; number of distinct groups of sensory neurons*/
	int gray_scales = Constants.gray_scales;
	/**interface for sensory neurons linked to this sensor. [discrete grayscale value][neuron id] */
	int[][] s_neurons; //eye_v;
	/** matrix of exact black and white values for the image [row][col] = grayscale value */
	double[][] bw;
	/** maps actual values in world to sensors (square sensory field, can be overlapping) 
	 * [sensor id][topmost sensor, leftmost sensor, size]*/
	int[][] eye_interface;
	
	//muscles
	/** eye motion: move right or move left (actually can happen at the same time, so should be separated)*/
	int[] eyemuscle_h = {-1,0,1};//I have decided to add 0.. but the choice not to do something should be different
	/** eye motion: move up or down*/
	int[] eyemuscle_v = {-1,0,1};
	/** resolution of eye  motion: how many pixels do we move at each motion?*/
	int eye_motion_res = 10;

	
	//UI
	BufferedImage image_input;
	BufferedImage eye_input;
	BufferedImage eye_input_coarse;
	
	public Eye(String imagesPath){
		//init
		this.imagesPath = imagesPath;
		
		//graphics
    	panel = new Surface();
    	
    	//number of neurons in focused area
    	n = ef_s*ef_s/(eres_f*eres_f);
		//number of neurons in non-focused area
		n+= ((vf_w*vf_h) - (ef_s*ef_s))/(eres_nf*eres_nf);//total n of pixels - focused pixels, / resolution
		
		//sensory neurons
		s_neurons = new int[gray_scales][n];
		//sensory field mapping to real world
		eye_interface = new int[n][3];		
		
		
		//build interface
		// total number of unit sensors (sites)
		int nn = 0;
		//width
		int w = 0;
		//height
		int h = 0;
		
		//do in focus first,left to right
		h = (vf_h-ef_s)/2;
		w = (vf_w-ef_s)/2;
		boolean next = true;
		while(next){
			eye_interface[nn][0] = h;//row
			eye_interface[nn][1] = w;//col
			eye_interface[nn][2] = eres_f;//size
			w+=eres_f;//next column
			if(w >= ((vf_w-ef_s)/2)+ef_s){
				//next row
				h+=eres_f;
				w=(vf_w-ef_s)/2;
			}		
			if(h >= ((vf_h-ef_s)/2)+ef_s){
				next = false;
			}
			nn++;
		}
	
		//now do outfocus
		h = 0;
		w = 0;
		//go down to next row
		next = true;
		while(next){
			boolean infocus = ( h>=(vf_h-ef_s)/2 & h<((vf_h-ef_s)/2)+ef_s) & (w>=(vf_w-ef_s)/2 & w<((vf_w-ef_s)/2)+ef_s);
			if(!infocus){
				eye_interface[nn][0] = h;//row
				eye_interface[nn][1] = w;
				eye_interface[nn][2] = eres_nf;//size
				w+=eres_nf;//next column
				nn++;
			} else{
				w+=ef_s;
			}
			if(w >= vf_w){
				//next row
				h+=eres_nf;
				w=0;
			}				
			if(h >= vf_h){
				next = false;
			}
		}
		
		mlog.say("eye interface sites "+ nn);
		
	}

	
	/**
	 * reads an image 
	 * builds the black and white buffer bw
	 * also initializes im_h, im_w and bw
	 * @param name name of the image file (without path)
	 * @return buffer of black and white values bw[??][??]
	 */
	public double[][] readImage(String name){
		
		try {
			image_input = ImageIO.read(new File(imagesPath+name+"_very_small.png"));
			im_h = image_input.getHeight();
			im_w = image_input.getWidth();	
			
			//black and white buffer for image
			//[row][column] = blackness level
			double[][] bw = new double[im_h][im_w];
			
			//build the whole image
			for(int i=0; i<im_h;i++){
				for(int j=0; j<im_w;j++){
					Color color = new Color(image_input.getRGB(j,i));
			        int b = color.getBlue();//0:255
			        int g = color.getGreen();
			        int r = color.getRed();
			        //convert to bw
			        double mean = (b+g+r)/(255*3.0);
			        //high value is black
			        double b2 = (1-mean);
			        bw[i][j] = b2;
				}				
			}			
			return bw;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/** 
	 * @return total number of neurons sensitive to one grayscale value
	 */
	public int getPartialNeuronsNumber(){
		return n;
	}


	/**
	 * use to build interface between eye and net.
	 * 
	 * @param nid id fo the neuron that will receive input
	 * @param scale the greyscale value this neuron is sensitive to
	 * @param index position of the neuron in the list of neurons sensitive to same greyscale value
	 */
	public void linkNeuron(int nid, int scale, int index) {
		//this sensor position i in this grayscale s links to this neuron		
		s_neurons[scale][index] = nid;
	}
	
	/**
	 * 
	 * @return the number of possible motions.
	 */
	public int getVerticalMotionResolution(){
		return eyemuscle_v.length;
	}
	
	/**
	 * 
	 * @return the number of possible motions.
	 */
	public int getHorizontalMotionResolution(){
		return eyemuscle_h.length;
	}

}
