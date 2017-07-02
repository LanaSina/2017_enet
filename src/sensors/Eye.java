package sensors;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.imageio.ImageIO;
import communication.Constants;
import communication.MyLog;
import graphics.Surface;

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
	/** becomes true after eye has been 1st initialized with an image*/
	boolean init = false;
	
	public boolean has_noise = true;
	public int noise_range = 50;
	public double noise_rate = 0.1;
	
	/** total number of neurons sensitive to one grayscale value*/
	int n;
	/**where to find images files*/
	String imagesPath;
	/** size of current image we're looking at*/
	int im_h,im_w;
	/** visual field size */
	int vf_h = Constants.vf_h, vf_w = Constants.vf_w;
	//int vf_h = 50, vf_w = 50;
	/** size of focused area */
	int ef_h = Constants.ef_h;
	int ef_w = Constants.ef_w;
	/** what the net sees */
	double[][] visual_field;//focus
	/** resolution of focused area = px/side of square */ 
	int eres_f = Constants.eres_f;
	/** resolution of non focused area = px/side of square */ 
	int eres_nf = Constants.eres_nf;
		
	/** sensitivity resolution; number of distinct groups of sensory neurons*/
	int gray_scales = Constants.gray_scales;
	/**interface for sensory neurons linked to this sensor. [discrete grayscale value][neuron id] */
	int[][] s_neurons;
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
	/** current center of focus in the image*/
	int[] focus_center = new int[2];
	/** vertical motion */
	int v_m = 0;
	/** horizontal motion*/
	int h_m = 0;
	
	//UI
	/** actual image from data folder*/
	BufferedImage image_input;
	/** limited area fitting eye size*/
	BufferedImage eye_input;
	/** actual eye input with coarse outfocus zone*/
	BufferedImage eye_input_coarse;
	
	/**
	 * 
	 * @param imagesPath
	 * @param panel
	 * @param vh visual field h
	 * @param vw visual field w
	 * @param eh focus size h
	 * @param ew focus size w
	 */
	public Eye(String imagesPath, Surface panel){
		//init
		this.imagesPath = imagesPath;
		this.panel = panel;
	
    	//number of neurons in focused area
    	n = ef_h*ef_w/(eres_f*eres_f);
    	mlog.say("n focus "+ n);
		//number of neurons in non-focused area
		n+= ((vf_w*vf_h) - (ef_h*ef_w))/(eres_nf*eres_nf);//total n of pixels - focused pixels, / resolution
		mlog.say("n plus non focus "+ n);
		
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
		h = (vf_h-ef_h)/2;
		w = (vf_w-ef_w)/2;
		boolean next = true;
		while(next){
			eye_interface[nn][0] = h;//row
			eye_interface[nn][1] = w;//col
			eye_interface[nn][2] = eres_f;//size
			w+=eres_f;//next column
			if(w >= ((vf_w-ef_w)/2)+ef_w){//
				//next row
				h+=eres_f;
				w=(vf_w-ef_w)/2;
				//mlog.say("next h "+ h);
			}		
			if(h >= ((vf_h-ef_h)/2)+ef_h){
				next = false;
			}
			nn++;
			if(nn>=n){
				next = false;
			}
		}
		
		//now do outfocus
		h = 0;
		w = 0;
		//go down to next row
		next = true;
		while(next){
			if(h >= vf_h || nn>=n){
				next = false;
			}
			if (!next) {
				continue;
			}
			boolean infocus = (h>=(vf_h-ef_h)/2 & h<((vf_h-ef_h)/2)+ef_h) & (w>=(vf_w-ef_w)/2 & w<((vf_w-ef_w)/2)+ef_w);
			if(!infocus){
				//mlog.say(" "+nn);
				eye_interface[nn][0] = h;//row
				eye_interface[nn][1] = w;
				eye_interface[nn][2] = eres_nf;//size
				w+=eres_nf;//next column
				nn++;
			} else{
				w+=ef_w;
			}
			if(w >= vf_w){
				//next row
				h+=eres_nf;
				w=0;
			}				
			
		}
		
		mlog.say("eye interface sites "+ nn);		
	}

	
	/**
	 * reads an image 
	 * builds the black and white buffer bw
	 * also initializes im_h, im_w and bw
	 * @param name name of the image file (without path)
	 * @return buffer of black and white values bw[h][w]
	 */
	public double[][] readImage(String name){
			
		try {
			String imagepath = imagesPath+name+Constants.image_format;
			mlog.say("reading " + imagepath);

			image_input = ImageIO.read(new File(imagepath));
			im_h = image_input.getHeight();
			im_w = image_input.getWidth();	
			
			//black and white buffer for image
			//[row][column] = blackness level
			//bw = new double[im_h][im_w];
			bw = new double[im_w][im_h];
			
			//build the whole image
			for(int i=0; i<im_w;i++){
				for(int j=0; j<im_h;j++){
					Color color = new Color(image_input.getRGB(i,j));
			        int b = color.getBlue();//0:255
			        int g = color.getGreen();
			        int r = color.getRed();
			        //convert to bw
			        double mean = (b+g+r)/(255*3.0);
			        if(has_noise){
			        	if(Constants.uniformDouble(0, 1)<noise_rate){
			        		mean = Constants.uniformDouble(mean-(noise_range/255.0), mean+(noise_range/255.0));
			        		if(mean<0) mean = 0;
			        		if(mean>1) mean = 1;
			        	}
			        }
			        //high value is black
			        double b2 = (1-mean);
			        bw[i][j] = b2;
				}				
			}
			//reset focus point only when eye first created
			if(init == false){
				focus_center[0] = im_w/2;//x
				focus_center[1] = im_h/2; //y
				init = true;
			}	
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return bw;
	}
	
	
	/**
	 * builds "image" of sensory input, a coarse version of the original image
	 * from the motion of eye muscles. We use relative motion bc it takes less memory space (easier)
	 * @return activation values on sensors [gray scale id]*total resolution. High values are black.
	 */
	public int[] buildCoarse(){
		
		int plus = Constants.eres_nf;
		eye_input = new BufferedImage(vf_w+plus, vf_h+plus, BufferedImage.TYPE_INT_RGB);
		eye_input_coarse = new BufferedImage(vf_w+plus, vf_h+plus, BufferedImage.TYPE_INT_RGB);
		
		//size of one grayscale sensitive layer
		int n = s_neurons[0].length;
		int[] coarse = new int[n];	
		
		//first, shift focus
		focus_center[0] +=  h_m*eye_motion_res;//x
		focus_center[1] +=  v_m*eye_motion_res;//y
		
		//limit conditions (cannot look out of image)
		//mlog.say("##### conditions " + Constants.w + " focus " + focus_center[0] + " " + vf_h);
		if((focus_center[0]-(vf_w/2))<0) focus_center[0] = 0;
		if((focus_center[0]+(vf_w/2))>Constants.w) focus_center[0] = Constants.w;
		if((focus_center[1]-(vf_h/2))<0) focus_center[1] = 0;
		if((focus_center[1]+(vf_h/2))>Constants.h) focus_center[1] = Constants.h;
		
		
		//top left corner
		int of_i = focus_center[0]-(vf_w/2);//x, col
		int of_j = focus_center[1]-(vf_h/2);//y, row

		//go through sensors via interface
		double[] sums = new double[n];
		for(int i=0; i<n; i++){
			sums[i] = 0;
		}	
		//calculate level of blackness of each sensory field
		for(int k=0; k<n; k++){//cool stuff can work with overlap too
			int sensor_j = eye_interface[k][0]+of_j;//row
			int sensor_i = eye_interface[k][1]+of_i;//col
			int size = eye_interface[k][2];//size of the zone for this sensor
			if(sensor_i>=0 & sensor_i+size<im_w & sensor_j>=0 & sensor_j+size<im_h){//?w h
				for(int i=sensor_i; i<sensor_i+size; i++){//row, x
					for(int j=sensor_j; j<sensor_j+size; j++){//col, y
						sums[k]+=bw[i][j];//[row][column] 
					}
				}		
			}
			//average it
			sums[k] = sums[k]/(size*size);
			
			//low sensitivity to darker shades
			double d = 0;
			if(sums[k]==1){//very black
				d = gray_scales;
			} else{
				d = sums[k]/(1.0/gray_scales);
				d = d+1; //+1 so that no stimulation is not treated as white???
			}	
			coarse[k] = (int)d;

				
			//build visualisation for UI
			int b = (int) (((1-sums[k])*255)+0.5);
			Color color = new Color(b,b,b); 
			//double div
			double val = coarse[k]/(1.0*gray_scales);// div by:  net can see how many values of grey (including white)
			val = 1 - val;
			b = (int) ((val*255)+0.5);
			Color color2 = new Color(b,b,b);
			int rel_j = eye_interface[k][0];//row, y
			int rel_i = eye_interface[k][1];//col, x
			//mlog.say("i " + rel_i + " j " + rel_j);
			for(int i=rel_i; i<rel_i+size; i++){//y
				for(int j=rel_j; j<rel_j+size; j++){//x	
					eye_input.setRGB(i, j, color.getRGB());//j and i
					eye_input_coarse.setRGB(i, j, color2.getRGB());
				}
			}
		}
		
		//set ui
		//if(panel!=null){
			panel.setComponents(image_input, eye_input, eye_input_coarse, focus_center);
		//}
		
		return coarse;
	}
	
	
	/** 
	 * @return total number of neurons sensitive to one grayscale value
	 */
	public int getPartialNeuronsNumber(){
		return n;
	}
	
	
	/**
	 * Embodiment: transform actions "intentions" into actual actions
	 * depending on how muscles actually act on the body.
	 * @param v_muscles index of muscles to activate (vertical eye motion)
	 * @param h_muscles index of muscles to activate (horizontal eye motion)
	 * @return index of muscles that were actually contracted (eq. to proprioception)
	 * [vertical, horizontal]
	 */
	public int[] contractMuscles(Vector<Integer> v_muscles,Vector<Integer> h_muscles){	
		v_m = 0;
		for(int i=0; i<v_muscles.size();i++){
			//the real action depends on the result of activating all involved muscles
			v_m += eyemuscle_v[v_muscles.get(i)];
			
			//check if action possible
			//see focus
			int a = focus_center[1] + v_m*eye_motion_res;//fph
			//limit conditions (cannot look out of image)
			if((a-(vf_w/2))<0) v_m = 0;
			if((a+(vf_w/2))>Constants.w) v_m = 0;
		
		}
		h_m = 0;
		for(int i=0; i<h_muscles.size();i++){
			//the real action depends on the result of activating all involved muscles
			h_m += eyemuscle_h[h_muscles.get(i)];
			
			//check if action possible
			//see focus
			int a = focus_center[0] + h_m*eye_motion_res;//fpw
			//limit conditions (cannot look out of image)
			if((a-(vf_h/2))<0) h_m = 0;
			if((a+(vf_h/2))>Constants.h) h_m = 0;
		}
		
		int[] r = {v_m,h_m};
		return r;
	}


	/**
	 * use to build interface between eye and net.
	 * 
	 * @param nid id of the neuron that will receive input
	 * @param scale the greyscale value this neuron is sensitive to
	 * @param index position of the neuron in the list of neurons sensitive to same greyscale value
	 */
	public void linkNeuron(int nid, int scale, int index) {
		//this sensor position i in this grayscale s links to this neuron		
		s_neurons[scale][index] = nid;
	}
	
	/**
	 * interface for sensory neurons linked to this sensor. 
	 * @return [discrete grayscale value][neuron id]
	 */
	public int[][] getNeuralInterface(){
		return s_neurons;
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

	/**
	 * 
	 * @return height size of sensor
	 */
	public int getH() {
		return vf_h;
	}
	
	/**
	 * 
	 * @return width of sensor
	 */
	public int getW() {
		return vf_w;
	}

	/**
	 * builds and displays the image corresponding to the image expected at t+1.
	 * @param coarse array of length (n_eye_place_sensors), continuous values 0:1. -1 for no prediction.
	 */
	public void setPredictedBuffer(double[] coarse) {
		/** predicted image */
		BufferedImage prediction = new BufferedImage(vf_w+Constants.eres_nf, vf_h+Constants.eres_nf, BufferedImage.TYPE_INT_RGB);
		
		for(int k=0; k<n; k++){
			int size = eye_interface[k][2];//size of the zone for this sensor
			//build visualisation for UI
			int b = (int) (((1-coarse[k])*255)+0.5);//+1 if eye sees white?
			Color color2;
			if(coarse[k]<0){
				color2 = new Color(0,0,200);//blue = no prediction
			}else{
				color2 = new Color(b,b,b);
			}
			int rel_i = eye_interface[k][0];//row, y
			int rel_j = eye_interface[k][1];//col, x
			for(int i=rel_i; i<rel_i+size; i++){
				for(int j=rel_j; j<rel_j+size; j++){		       
					prediction.setRGB(j, i, color2.getRGB());
				}
			}
		}
		
		if(panel!=null){
			panel.setPredicted(prediction);
		}
	}


	public int[][] getEyeInterface() {
		return eye_interface;
	}
	
	
}
