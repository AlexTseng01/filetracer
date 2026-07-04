package com.alex.filetracer;

public interface ScanListener {
	void onProgress(int filesProcessed);
	void onComplete(double seconds);
}
