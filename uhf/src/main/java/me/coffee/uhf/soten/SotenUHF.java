package me.coffee.uhf.soten;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.widget.Toast;

import com.soten.libs.base.MessageResult;
import com.soten.libs.uhf.UHFManager;
import com.soten.libs.uhf.UHFResult;
import com.soten.libs.uhf.base.MessageTran;
import com.soten.libs.uhf.base.ResultBundle;
import com.soten.libs.uhf.impl.UHF;
import com.soten.libs.uhf.impl.UHFExpress;
import com.soten.libs.uhf.impl.UHFModelListener;
import com.soten.libs.utils.PowerManagerUtils;
import com.soten.libs.utils.StringUtils;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

/**
 * UHF封装类
 */
public class SotenUHF implements UHFModelListener {

    private UHFManager mUHFManager;
    private UHF uhf;
    private UHFExpress mUHFExpress;
    private Method mSendCmd;
    private Context context;

    private static final byte[] readTidCMD = new byte[]{(byte) 0x02, (byte) 0x00, Byte.parseByte("6"),
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private Timer mTimer;
    private TimerTask mTimerTask;
    private int max = -1;
    private SotenListener mListener;
    private boolean isScan;

    private SotenUHF() {
    }

    private static final class Holder {
        static final SotenUHF instance = new SotenUHF();
    }

    public static SotenUHF getInstance() {
        return Holder.instance;
    }

    public void setListener(SotenListener listener) {
        this.mListener = listener;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        mUHFManager = UHFManager.getInstance();
        uhf = mUHFManager.getUHF();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManagerUtils.open(pm, 0x0C);
    }

    public void start() {
        if (mUHFManager.isOpen()) return;
        mUHFManager.open(context);
        mUHFManager.register(this);
    }

    public void stop() {
        isScan = false;
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void close() {
        stop();
        mUHFManager.unregister(this);
        mUHFManager.close(uhf, context);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManagerUtils.close(pm, 0x0C);

    }

    public void read(int max) {
        if (!mUHFManager.isOpen()) {
            Toast.makeText(context, "请打开UHF再扫描", Toast.LENGTH_SHORT).show();
            return;
        }
        isScan = true;
        this.max = max;
        if (max == 1) {
            getTID();
            countDownTimer.start();
        } else if (max >= 0) {
            if (mTimer == null) mTimer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    getTID();
                }
            };
            mTimer.schedule(mTimerTask, 0, 60);
        }
    }

    private void getTID() {
        try {
            if (mSendCmd == null) {
                Method expMethod = UHF.class.getDeclaredMethod("getExpress");
                expMethod.setAccessible(true);
                mUHFExpress = (UHFExpress) expMethod.invoke(uhf);
                mSendCmd = UHFExpress.class.getDeclaredMethod("sendMessage", byte[].class);
                mSendCmd.setAccessible(true);
            }
            MessageTran messageTran = new MessageTran((byte) 0xFF, (byte) -127, readTidCMD);
            mSendCmd.invoke(mUHFExpress, messageTran.getAryTranData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceice(MessageResult result) {
        UHFResult uhfResult = (UHFResult) result;
        Bundle bundle = uhfResult.getBundle();
        int cmd = bundle.getInt(ResultBundle.CMD);
        if (cmd == -127) {
            byte[] data = bundle.getByteArray(ResultBundle.DATA);
            if (null != data && data.length > 0) {
                String tid = StringUtils.toHexString(data).toUpperCase();
                tid = tid.replaceAll("\\s*", "");
                finish(tid);
            } else if (max == 1 && isScan) {
                getTID();
            }
        }
    }

    private void finish(String tid) {
        if (mListener != null && max > 0) mListener.onReceived(tid);
        if (max == 1) {
            max = -1;
            stop();
        }
    }

    private CountDownTimer countDownTimer = new CountDownTimer(1500, 500) {

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (isScan) finish(null);
        }
    };

    @Override
    public void onLostConnect(Exception e) {

    }

    public interface SotenListener {

        void onReceived(String value);
    }
}
