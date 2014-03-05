package com.eit.bluetooth;

public interface BluetoothCommunication {
    /**
     * Start to move forward, then stopping and go idle.
     * @param distance Number of units to move forward,
     */
    void move(int distance);

    /**
     * Rotate a number of degrees and then stop. Positive value indicates right rotation
     * Negative value indicates left.
     * @param degrees Number of degrees to rotate
     */
	void rotate(double degrees);
	
	void openClaw();
	
	void closeClaw();

    /**
     * @return True if none of the engines are running
     */
    boolean isIdle();
	
	boolean isSensorPressed();
}
