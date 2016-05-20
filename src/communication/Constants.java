package communication;

/**
 * Global variables
 * @author lana
 *
 */
public class Constants {
	
	/** controls output of all loggers. Set true for verbose mode, false for dry mode.*/
	public static final boolean shouldLog = true;


	/** age at which probabilistic connections stop learning */
	public static final int weight_max_age = 100;
	/** confidence threshold for predictions */
	public static final double confidence_threshold = 0.9;//90%
	
	/** sensitivity of the image sensor */
	public static final int gray_scales = 4;


	//weight types
	/** this connection weight will always be max_weight (=1)*/
	public static final int fixedConnection = 1;
	/** this connection weight will change with time*/
	public static final int defaultConnection = 2;
	
	
}
