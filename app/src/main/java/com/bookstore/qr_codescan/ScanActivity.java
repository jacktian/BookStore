package com.bookstore.qr_codescan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;

import com.bookstore.bookparser.BookData;
import com.bookstore.bookparser.BookInfoJsonParser;
import com.bookstore.connection.BookInfoRequestBase;
import com.bookstore.connection.BookInfoUrlBase;
import com.bookstore.connection.douban.DoubanBookInfoUrl;
import com.bookstore.main.R;
import com.bookstore.qr_codescan.danmakuFlame.master.flame.danmaku.controller.IDanmakuView;
import com.bookstore.qr_codescan.danmakuFlame.master.flame.danmaku.danmaku.model.BaseDanmaku;
import com.bookstore.qr_codescan.zxing.camera.CameraManager;
import com.bookstore.qr_codescan.zxing.decoding.CaptureActivityHandler;
import com.bookstore.qr_codescan.zxing.decoding.InactivityTimer;
import com.bookstore.qr_codescan.zxing.view.ViewfinderView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Initial the camera
 *
 * @author Ryan.Tang
 */
public class ScanActivity extends Activity implements Callback {

    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;
    private static int pos = 0;
    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private boolean vibrate;
    private Danmu mDanmu;
    private IDanmakuView danmu_View;
    private ArrayList<BookData> scanedBookList = new ArrayList<BookData>();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        CameraManager.init(getApplication());
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        danmu_View = (IDanmakuView) findViewById(R.id.danmu_view);

        mDanmu = new Danmu(this);
        mDanmu.initDanmuView(danmu_View);

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);

        final Handler danmuHandler = new Handler();
        danmuHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!scanedBookList.isEmpty()) {
                    mDanmu.addDanmuWithTextAndImage(false, scanedBookList.get(pos));
                    pos++;
                    pos = pos % scanedBookList.size();
                }
                danmuHandler.postDelayed(this, getDelayTime(scanedBookList.size()));
            }
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;

        mDanmu.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();

        mDanmu.pause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        scanedBookList.clear();
        mDanmu.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!scanedBookList.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String alertTitle = getResources().getString(R.string.scan_alert_title, scanedBookList.size());
            builder.setTitle(alertTitle);
            builder.setPositiveButton(R.string.positive_OK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ScanActivity.this.finish();
                }
            });
            builder.setNegativeButton(R.string.negative_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * ����ɨ����
     *
     * @param result
     */
    public void handleDecode(Result result) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        String resultString = result.getText();
//        if (resultString.equals("")) {
//            Toast.makeText(ScanActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
//        } else {
//            Intent resultIntent = new Intent();
//            Bundle bundle = new Bundle();
//            bundle.putString("result", resultString);
//            resultIntent.putExtras(bundle);
//            this.setResult(RESULT_OK, resultIntent);
//        }
        //ScanActivity.this.finish();
        //mDanmu.addDanmuText(false, resultString);
        getBookData(resultString);
        Handler restartHandler = new Handler();
        restartHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (handler != null) {
                    handler.restartPreviewAndDecode();
                }
            }
        }, 3000);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    public void getBookData(final String isbn) {
        if (isbn == null) {
            Log.i("BookStore", "isbn is null");
            return;
        }
        Log.i("BookStore", "isbn is " + isbn);
        DoubanBookInfoUrl doubanBookUrl = new DoubanBookInfoUrl(isbn);
        BookInfoRequestBase bookRequest = new BookInfoRequestBase(doubanBookUrl) {
            @Override
            protected void requestPreExecute() {

            }

            @Override
            protected void requestPostExecute(String bookInfo) {
                try {
                    BookData bookData = BookInfoJsonParser.getInstance().getSimpleBookDataFromString(bookInfo);
                    //Bitmap bitmap = ((BitmapDrawable) (getResources().getDrawable(R.drawable.downloading_smallcover))).getBitmap();
                    //mDanmu.addDanmuWithTextAndImage(false, bookData);
                    //LoadBookSmallImage(bookData);
                    scanedBookList.add(bookData);
                    mDanmu.addDanmuText(BaseDanmaku.TYPE_FIX_BOTTOM, false, bookData.title);
                    //mDanmu.addDanmuText(BaseDanmaku.TYPE_SCROLL_LR, false, bookData.title);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        bookRequest.requestExcute(BookInfoUrlBase.REQ_ISBN);
    }

    public void LoadBookSmallImage(final BookData bookData, final BaseDanmaku danmaku) {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(false)
                .build();
        ImageLoader.getInstance().loadImage(bookData.images_small, options, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                BitmapDrawable drawable = new BitmapDrawable(loadedImage);
                int width = 0;
                int height = 0;
                if (loadedImage != null) {
                    width = loadedImage.getWidth();
                    height = loadedImage.getHeight();
                } else {
                    width = 75;
                    height = 100;
                }

                if (drawable != null) {
                    drawable.setBounds(0, 0, width, height);
                    SpannableStringBuilder spannable = mDanmu.createSpannable(drawable, bookData.title);
                    danmaku.text = spannable;
                    danmu_View.invalidateDanmaku(danmaku, false);
                }
            }
        });
    }

    public int getDelayTime(int scanCount) {
        if (scanCount >= 0 && scanCount <= 1) {
            return 5000;//delay 5s
        } else if (scanCount >= 2 && scanCount <= 3) {
            return 4000;//delay 4s
        } else if (scanCount >= 4 && scanCount <= 5) {
            return 3000;//delay 3s
        } else if (scanCount >= 6 && scanCount <= 7) {
            return 2000;//delay 2s
        } else if (scanCount >= 8 && scanCount <= 10) {
            return 1000;//delay 1s
        } else {
            return 500;//delay 0.5s
        }
    }
}