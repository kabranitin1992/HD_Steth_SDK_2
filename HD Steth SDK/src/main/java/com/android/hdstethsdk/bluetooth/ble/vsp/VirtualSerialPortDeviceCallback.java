package com.android.hdstethsdk.bluetooth.ble.vsp;

public interface VirtualSerialPortDeviceCallback
{
	/**
	 * Callback indicating if the VSP service was found or not on the remote BLE
	 * device
	 * 
	 * @param found
	 *            true if VSP service was found, otherwise false
	 */
	public void onUiVspServiceFound(final boolean found);

	/**
	 * Callback that notifies us that the data was sent successfully to the
	 * remote BLE device
	 * 
	 * @param dataSend
	 *            the value that was successfully send to the remote BLE device
	 */
	public void onUiSendDataSuccess(final String dataSend);

	/**
	 * Callback for when data is received from the remote device
	 * 
	 * @param dataReceived
	 *            the value that was received from the remote device
	 */
	//public void onUiReceiveData(final String dataReceived);

	public void onUiReceiveData(final byte[] dataReceived);

	public void onUiUploaded();
}