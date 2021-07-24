
package com.android.hdstethsdk.bluetooth.ble;

public interface IBleBaseActivityUiCallback
{

	public void onUiConnected();

	public void onUiConnecting();

	public void onUiDisconnected(final int status);

	public void onUiDisconnecting();

	public void onUiBatteryRead(final String valueBattery);

	public void onUiReadRemoteRssi(final int valueRSSI);
}