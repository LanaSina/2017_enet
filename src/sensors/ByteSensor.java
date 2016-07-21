package sensors;

import graphics.Surface;

public class ByteSensor {

	public ByteSensor(String imagesPath, Surface panel){
		//init
		this.imagesPath = imagesPath;
		this.panel = panel;
		
		//graphics
    	/*panel = new Surface();*/
    	
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
}
