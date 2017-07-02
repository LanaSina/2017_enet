package communication;

import java.io.File;

import javax.swing.JButton;

/**
 * Interface for threads to be controlled by the Surface UI
 * @author lana
 *
 */
public interface ControllableThread {
	public void setPaused(boolean paused);
	
	public int speedUp();
	
	public int speedDown();
	
	public void save(JButton saveButton);
	
	public void refresh();

	public void load(File file);
}
