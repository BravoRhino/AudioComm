/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.libra.sinvoice;

import android.util.Log;

import com.libra.sinvoice.Buffer.BufferData;

public class VoiceRecognition {
    private final static String TAG = "Recognition";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private final static int STEP1 = 1;
    private final static int STEP2 = 2;
//    private final static int INDEX[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1, 8, -1, -1, -1, 7, -1, -1, 6, -1, -1, 5, -1, -1, 4, -1, -1,
//            3, -1, -1, 2, -1, -1, 1, -1, -1, 0 };

//    private final static int INDEX[] =  { -1, -1, -1, -1, -1, -1, 17, -1, -1, -1, 16, -1, -1, 15, -1, -1, 14, -1, -1, 13, -1, -1, 12, -1, -1, 11, -1, -1,
//            10, -1,-1,9,-1,-1,8,-1,-1,7,-1,-1,6,-1,-1,5,-1,-1,4,-1,-1,3,-1,-1,2,-1,-1,1,-1,-1,0};
//private final static int INDEX[] =  { -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, 8, -1, -1, 7, -1, -1, 6, -1, -1, 5, -1, -1, 4, -1, -1, 3, -1, -1,
//        2, -1,-1,1,-1,-1,0};
//    private final static int MAX_SAMPLING_POINT_COUNT = 34;
//    private final static int MIN_REG_CIRCLE_COUNT = 6;
    private FFT fft = new FFT();

    private int mState;
    private Listener mListener;
    private Callback mCallback;

    private int mSamplingPointCount = 0;

    private int mSampleRate;
    private int mChannel;
    private int mBits;

    private boolean mIsStartCounting = false;
    private int mStep;
    private boolean mIsBeginning = false;
    private boolean mStartingDet = false;
    private int mStartingDetCount;

    private int mRegValue;
    private int regFreq1;
    private int regFreq2;
    private int mRegIndex1;
    private int mRegIndex2;
    private int mRegCount;
    private int mPreRegCircle1 = 0;
    private int mPreRegCircle2 = 0;
    private boolean mIsRegStart = false;

    public static interface Listener {
        void onStartRecognition();

        void onRecognition(int index1, int index2);

        void onStopRecognition();
    }

    public static interface Callback {
        BufferData getRecognitionBuffer();

        void freeRecognitionBuffer(BufferData buffer);
    }

    public VoiceRecognition(Callback callback, int SampleRate, int channel, int bits) {
        mState = STATE_STOP;

        mCallback = callback;
        mSampleRate = SampleRate;
        mChannel = channel;
        mBits = bits;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void start() {
        if (STATE_STOP == mState) {

            if (null != mCallback) {
                mState = STATE_START;
                mSamplingPointCount = 0;

                mIsStartCounting = false;
                mStep = STEP1;
                mIsBeginning = false;
                mStartingDet = false;
                mStartingDetCount = 0;
                mPreRegCircle1 = -1;
                mPreRegCircle2 = -1;
                if (null != mListener) {
                    mListener.onStartRecognition();
                }
                while (STATE_START == mState) {
                    BufferData data = mCallback.getRecognitionBuffer();

                    if (null != data) {
                        if(data.mData==null)
                            Log.d("Data Info: ", "empty data");
                        if (null != data.mData) {
                            process(data);
                            Log.d("Data Info: ", "valid data");
                            mCallback.freeRecognitionBuffer(data);
                        } else {
                            LogHelper.d(TAG, "end input buffer, so stop");
                            break;
                        }
                    } else {
                        LogHelper.e(TAG, "get null recognition buffer");
                        break;
                    }
                }

                mState = STATE_STOP;
                if (null != mListener) {
                    mListener.onStopRecognition();
                }
            }
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;
        }
    }

    private void process(BufferData data) {
        int size = data.getFilledSize() - 1;
        double[] spectrum ;
        int freq1 = 0;
        int freq2 = 0;
        double[] dataBlock = new double[(int)size/2+1];
        double sh = 0;
        for (int i = 0; i < size; i++) {
            short sh1 = data.mData[i];
            sh1 &= 0xff;
            short sh2 = data.mData[++i];
            sh2 <<= 8;
            sh = (double) ((sh1) | (sh2));
            dataBlock[(i-1)/2] = sh;
        }
        double[] partData = new double[256];

        spectrum = fft.magnitudeSpectrum(dataBlock);
        double Max1 = spectrum[100];

        for(int i = 100;i<111;i++){
            if(Max1<spectrum[i]){

                Max1 = spectrum[i];
                freq1 = i;
            }
//            else if(Max2 <spectrum[i]){
//                Max2 = spectrum[i];
//                freq2 = i;
//            }
        }
        Log.d("Max power frequency: ",freq1+";"+freq2);

        if(freq1==103||freq1==104||freq1==105||freq1==106||freq1==107||freq1==108||freq1==109){
            if (null != mListener) {

                switch (freq1){
                    case 103:
                        mRegIndex1 = 0;
                        break;
                    case 104:
                        mRegIndex1 = 1;
                        break;
                    case 105:
                        mRegIndex1 = 2;
                        break;
                    case 106:
                        mRegIndex1 = 3;
                        break;
                    case 107:
                        mRegIndex1 = 4;
                        break;
                    case 108:
                        mRegIndex1 = 5;
                        break;
                    case 109:
                        mRegIndex1 = 6;
                        break;
                }
                if(mRegIndex1!=mPreRegCircle1&&mRegIndex1!=6){
                   // regFreq1 = mRegIndex1;
                    mListener.onRecognition(mRegIndex1, mRegIndex2);
                }
                mPreRegCircle1 = mRegIndex1;

            }
        }



//        spectrum = FFT(dataBlock,(int)size/2+1);

    }
//    private void reg(int samplingPointCount) {
//        if (!mIsBeginning) {
//            if (!mStartingDet) {
//                if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
//                    mStartingDet = true;
//                    mStartingDetCount = 0;
//                }
//            } else {
//                if (MAX_SAMPLING_POINT_COUNT == samplingPointCount) {
//                    ++mStartingDetCount;
//
//                    if (mStartingDetCount >= MIN_REG_CIRCLE_COUNT) {
//                        mIsBeginning = true;
//                        mIsRegStart = false;
//                        mRegCount = 0;
//                    }
//                } else {
//                    mStartingDet = false;
//                }
//            }
//        } else {
//            if (!mIsRegStart) {
//                if (samplingPointCount > 0) {
//                    mRegValue = samplingPointCount;
//                    mRegIndex = INDEX[samplingPointCount];
//                    mIsRegStart = true;
//                    mRegCount = 1;
//                }
//            } else {
//                if (samplingPointCount == mRegValue) {
//                    ++mRegCount;
//
//                    if (mRegCount >= MIN_REG_CIRCLE_COUNT) {
//                        // ok
//                        if (mRegValue != mPreRegCircle) {
//                            if (null != mListener) {
//                                mListener.onRecognition(mRegIndex);
//                            }
//                            mPreRegCircle = mRegValue;
//                        }
//
//                        mIsRegStart = false;
//                    }
//                } else {
//                    mIsRegStart = false;
//                }
//            }
//        }
//    }

//    public short[] FFT(short[] dataBlock, int size){
//        short[] xConv;
//        int m = 11, j=2048;
//        if(size<1||size>2048){
//            return null;
//        }
//        xConv = new short[j];
//        for(int i=0;i<size;i++){
//            xConv[i] = dataBlock[i];
//        }
//        if(j>size){
//            for(int i=size;i<j;i++){
//                xConv[i] = 0;
//            }
//        }
//        //i2Sort(xConv,m);
//        //myFFT(xConv,m);
//
//        return dataBlock;
//    }


//    private int preReg(int samplingPointCount) {
//        //Log.i("Counting: ", samplingPointCount+"");
//        switch (samplingPointCount) {
////            case 5:
//            case 6:
//            case 7:
//            case 8:
//                samplingPointCount = 7;
//                break;
//            case 9:
//            case 10:
//            case 11:
//            case 12:
//                samplingPointCount = 10;
//                break;
//
//            case 13:
//            case 14:
//            case 15:samplingPointCount = 13;
//                break;
//            case 16:
//            case 17:
//
//            case 18:samplingPointCount = 16;
//                break;
//            case 19:
//            case 20:
//                samplingPointCount = 19;
//                break;
//
//            case 21:
//            case 22:
//            case 23:
//                samplingPointCount = 22;
//                break;
//
//            case 24:
//            case 25:
//            case 26:
//                samplingPointCount = 25;
//                break;
//
//            case 27:
//            case 28:
//            case 29:
//                samplingPointCount = 28;
//                break;
//
//            case 30:
//            case 31:
//            case 32:
//                samplingPointCount = 31;
//                break;
//
//
//            case 33:
//            case 34:
//            case 35:
//                samplingPointCount = 34;
//                break;

//            case 36:
//            case 37:
//            case 38:
//                samplingPointCount = 37;
//                break;
//
//            case 39:
//            case 40:
//            case 41:
//                samplingPointCount = 40;
//                break;
//            case 42:
//            case 43:
//            case 44:
//                samplingPointCount = 43;
//                break;
//
//            case 45:
//            case 46:
//            case 47:
//                samplingPointCount = 46;
//                break;
//            case 48:
//            case 49:
//            case 50:
//                samplingPointCount = 49;
//                break;
//
//            case 51:
//            case 52:
//            case 53:
//                samplingPointCount = 52;
//                break;
//
//            case 54:
//            case 55:
//            case 56:
//                samplingPointCount = 55;
//                break;
//
//            case 57:
//            case 58:
//            case 59:
//                samplingPointCount = 58;
//                break;
//
//
//        default:
//            samplingPointCount = 0;
//            break;
//        }
//
//        return samplingPointCount;
//    }


}
