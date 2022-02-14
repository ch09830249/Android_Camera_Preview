package com.mobiledrivetech.arhud.carpreview

import android.util.Log

class TakePhotoThread(private val task: TakePhoto): Thread() {

    private val TAG = "TakePhotoThread"
    private var status = Type.SUSPEND   // Does not start to capture the photos

    // Three status of this thread
    enum class Type {
        STOP, SUSPEND, RUNNING
    }

    // Set the thread status
    fun mStop () {
        status = Type.STOP
    }

    fun mSuspend () {
        status = Type.SUSPEND
    }

    fun mRunning () {
        status = Type.RUNNING
    }

    override fun run() {
        // If the status is Type.STOP, this task will end
        while (status != Type.STOP) {
            // If the status is Type.SUSPEND, this task will enter the infinite loop and do nothing
            if (status == Type.SUSPEND) {
                try {
                    sleep(1)
                }
                catch (e: InterruptedException) {
                    Log.e(TAG, e.toString())
                }
            }
            // If the status is Type.RUNNING, this task will take the photo constantly
            else {
                while (status == Type.RUNNING) {
                    task.takePhoto()
                    try {
                        sleep(500)
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }
            }
        }
    }
}