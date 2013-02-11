package com.wiley.wroxaccessories;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

public class UsbConnection12 extends Connection {
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;
	private ParcelFileDescriptor mFileDescriptor;
	private Activity mActivity;

	public UsbConnection12(UsbManager usbmanager) {
		UsbAccessory[] accessories = manager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			mUsbAccessory = accessory;
			if (manager.hasPermission(mUsbAccessory)) {
				mFileDescriptor = usbmanager.openAccessory(accessory);
				if (mFileDescriptor != null) {
					FileDescriptor mFileDescriptor = mFileDescriptor
							.getFileDescriptor();
					mFileInputStream = new FileInputStream(mFileDescriptor);
					mFileOutputStream = new FileOutputStream(mFileDescriptor);
				}
			}
		}
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		mActivity.registerReceiver(mBroadcastReceiver, mIntentFilter);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return mFileInputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return mFileOutputStream;
	}

	@Override
	public void close() throws IOException {
		if (mFileDescriptor != null) {
			mFileDescriptor.close();
		}
		mActivity.unregisterReceiver(mBroadcastReceiver);
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
			}
		}
	};
}
