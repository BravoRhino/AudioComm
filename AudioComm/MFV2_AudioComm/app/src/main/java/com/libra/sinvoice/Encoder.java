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

import java.util.ArrayList;
import java.util.List;

import com.libra.sinvoice.Buffer.BufferData;

public class Encoder implements SinGenerator.Listener, SinGenerator.Callback {
    private final static String TAG = "Encoder";
    private final static int STATE_ENCODING = 1;
    private final static int STATE_STOPED = 2;

    // index 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
    // sampling point Count 31, 28, 25, 22, 19, 15, 10
    //private final static int[] CODE_FREQUENCY = { 1102, 1191, 1297, 1422, 1575, 1764, 2004, 2321, 2940, 4410 };
    //private final static int[] CODE_FREQUENCY = { 1050,1160, 1297, 1470, 1696, 2004, 2450, 3150, 4410, 6300 };
    private final static int[] CODE_FREQUENCY = { 17851, 18174,18497,18820,19143,19466,19789,19294,19638};
    private int mState;

    private SinGenerator mSinGenerator;
    private Listener mListener;
    private Callback mCallback;

    public static interface Listener {
        void onStartEncode();

        void onEndEncode();
    }

    public static interface Callback {
        void freeEncodeBuffer(BufferData buffer);

        BufferData getEncodeBuffer();
    }

    public Encoder(Callback callback, int sampleRate, int bits, int bufferSize) {
        mCallback = callback;
        mState = STATE_STOPED;
        mSinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
        mSinGenerator.setListener(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public final static int getMaxCodeCount() {
        return CODE_FREQUENCY.length;
    }

    public final boolean isStoped() {
        return (STATE_STOPED == mState);
    }

    // content of input from 0 to (CODE_FREQUENCY.length-1)
//    public void encode(List<Integer> codes, int duration) {
//        encode(codes, duration, 0);
//    }

    public void encode(List<Character> codes, int duration, int muteInterval) {
        List<FreqPair> freqPairs;
        if (STATE_STOPED == mState) {
            mState = STATE_ENCODING;

            if (null != mListener) {
                mListener.onStartEncode();
            }
            freqPairs  = freqEncoding(codes);
            mSinGenerator.start();
            for (FreqPair pair  : freqPairs) {
                if (STATE_ENCODING == mState) {
                    //LogHelper.d(TAG, "encode:" + index);
                    //if (index >= 0 && index < CODE_FREQUENCY.length) {
                        mSinGenerator.gen(pair, duration);
                    Log.d("Frequency Encoding",pair.toString() );
                    //} else {
                      //  LogHelper.e(TAG, "code index error");
                    //}
                } else {
                    LogHelper.d(TAG, "encode force stop");
                    break;
                }
            }
            // for mute
            if (STATE_ENCODING == mState) {
                mSinGenerator.gen(new FreqPair(0,0), muteInterval);
            } else {
                LogHelper.d(TAG, "encode force stop");
            }
            stop();

            if (null != mListener) {
                mListener.onEndEncode();
            }
        }
    }

    public List<FreqPair> freqEncoding(List<Character> charList){
        List freqList = new ArrayList<FreqPair>();
        for(char x: charList){
        freqList.add(exchange(x));
        }

        return freqList;
    }
    public FreqPair exchange(Character ch){
        switch (ch){
            case 'S':
                return new FreqPair(0,1,true);
            case '0':
                return new FreqPair(1,2,true);
            case '1':
                return new FreqPair(2,3,true);
            case '2':
                return new FreqPair(3,4,true);
            case '3':
                return new FreqPair(4,5,true);
            case '4':
                return new FreqPair(5,6,true);
            case '5':
                return new FreqPair(0,2,true);
            case '6':
                return new FreqPair(1,3,true);
            case '7':
                return new FreqPair(2,4,true);
            case '8':
                return new FreqPair(3,5,true);
            case '9':
                return new FreqPair(4,6,true);
            case 'A':
                return new FreqPair(0,3,true);
            case 'B':
                return new FreqPair(4,1,true);
            case 'C':
                return new FreqPair(5,2,true);
            case 'D':
                return new FreqPair(6,3,true);
            case 'E':
                return new FreqPair(0,4,true);
            case 'F':
                return new FreqPair(1,5,true);
            case 'O':
                return new FreqPair(0,5,true);
            case 'V':
                return new FreqPair(6,1,true);
            case 'I':
                return new FreqPair(7,8,true);
            default:
                return null;
        }
    }


    public void stop() {
        if (STATE_ENCODING == mState) {
            mState = STATE_STOPED;

            mSinGenerator.stop();
        }
    }

    @Override
    public void onStartGen() {
        LogHelper.d(TAG, "start gen codes");
    }

    @Override
    public void onStopGen() {
        LogHelper.d(TAG, "end gen codes");
    }

    @Override
    public BufferData getGenBuffer() {
        if (null != mCallback) {
            return mCallback.getEncodeBuffer();
        }
        return null;
    }

    @Override
    public void freeGenBuffer(BufferData buffer) {
        if (null != mCallback) {
            mCallback.freeEncodeBuffer(buffer);
        }
    }
}
