package communication;

import java.util.Random;

/**
 * Global variables
 * @author lana
 *
 */
public class Constants {
	
	/** controls output of all loggers. Set true for verbose mode, false for dry mode.*/
	public static final boolean shouldLog = true;

	public static final String image_format = ".png";

	/** age at which probabilistic connections stop learning */
	public static final int weight_max_age = 25;//20
	/** confidence threshold for predictions */
	public static final double confidence_threshold = 0.95;//
	
	/** sensitivity of the image sensor */
	public static final int gray_scales = 6;
	/** resolution of focused area of eye*/
	public static final int eres_f = 2;
	public static final int eres_nf = 5;
	/**size of focused area*/
	public static final int ef_h = 20;
	public static final int ef_w = 20;
	/** size of visual field */
	public static final int vf_h = 50;
	public static final int vf_w = 50;
	/** image dimensions*/
	public static final int w = 80;//141;
	public static final int h = 80;//134;
	/** image dimensions (viewed from eye) */
	public static final int iw = (w/eres_f)*eres_f;
	public static final int ih = (h/eres_f)*eres_f;//round down depending on focus resolution
	//public static final int n_images = 83;

	
	/** snap every x timesteps*/
	public static final int snap_freq = 50;


	//weight types
	/** this connection weight will always be max_weight (=1)*/
	public static final int fixedConnection = 1;
	/** this connection weight will change with time*/
	public static final int defaultConnection = 2;

	/** where data files will be created*/
	public static final String DataPath = "/Users/lana/Development/SNET_data/";
	/** network parameters file*/
	public static final String Net_param_file_name = "net_parameters.csv";
	public static String Param_file_name = "parameters.csv";


	public static final String PerfFileName = "performance.csv";

	/** how much error is tolerated to call 2 neurons "same"*/
	public static double w_error = 0.1;

	
	//functions
	
	/**
	 * from http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
	 * and http://stackoverflow.com/questions/3680637/how-to-generate-a-random-double-in-a-given-range
	 * Returns a pseudo-random number between min and max, inclusive.
	 * Uniform distribution.
	 * The difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min Minimum value
	 * @param max Maximum value.  Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static double uniformDouble(double min, double max) {
	    // NOTE: Usually this should be a field rather than a method
	    // variable so that it is not re-seeded every call.
	    Random rand = new Random();
	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = min + (max - min) * rand.nextDouble();
	    return randomNum;
	}
	
}
