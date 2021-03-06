package com.android.hdstethsdk.bluetooth.ble.vsp;

import android.app.Activity;
import android.net.Uri;

import com.android.hdstethsdk.bluetooth.ble.IBleBaseActivityUiCallback;
import com.android.hdstethsdk.utility.FileWrapper;


public abstract class FileAndFifoAndVspManager extends VirtualSerialPortDevice
{
	protected static final int MAX_DATA_TO_READ_FROM_TEXT_FILE = 40;
	protected FileWrapper mFileWrapper;
	protected FileState mFileState;

	public FileAndFifoAndVspManager(Activity activity,
			IBleBaseActivityUiCallback iBleBaseActivityUiCallback)
	{
		super(activity, iBleBaseActivityUiCallback);

		mFileState = FileState.FILE_NOT_CHOSEN;
	}

	public FileState getFileState()
	{
		return mFileState;
	}

	public FileWrapper getFileWrapper()
	{
		return mFileWrapper;
	}

	/**
	 * The possible states for when a file is chosen
	 * 
	 * states: {@link FileState#FILE_CHOSEN} or
	 * {@link FileState#FILE_NOT_CHOSEN}
	 */
	public enum FileState
	{
		FILE_NOT_CHOSEN, FILE_CHOSEN
	}

	@Override
	protected void onCharsFoundCompleted()
	{
		super.onCharsFoundCompleted();

		if (isValidVspDevice())
		{
			if (mFileState == FileState.FILE_CHOSEN)
			{
				mFifoAndVspManagerState = FifoAndVspManagerState.READY_TO_SEND_DATA;
			}
		}
	}

	/**
	 * stores the file in a FileWrapper object based on the Uri parameter and
	 * initialises it to its default values
	 * 
	 * @param uri
	 *            the uri of the file to be initialised
	 */
	public void setFile(Uri uri)
	{
		if (uri == null)
			return;
		if (mFileWrapper != null)
		{
			mFileWrapper = null;
		}
		mFileWrapper = new FileWrapper(uri, mActivity);

		mFileState = FileState.FILE_CHOSEN;

		if (isValidVspDevice() == true)
		{
			mFifoAndVspManagerState = FifoAndVspManagerState.READY_TO_SEND_DATA;
		}
	}

	/**
	 * initialise state for sending data to the remote BLE device
	 */
	public void initialiseFileTransfer()
	{
		if (getBluetoothGatt() == null)
			return;

		mFifoAndVspManagerState = FifoAndVspManagerState.UPLOADING;
		mFileWrapper.setToDefaultValues();
	}
}