package io.github.RoboTime.rectangledetection;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.graphics.Matrix;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.Random;

import io.github.RoboTime.rectangledetection.models.CameraData;
import io.github.RoboTime.rectangledetection.views.CameraPreview;
import io.github.RoboTime.rectangledetection.views.DrawView;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private class points
    {
        public int x,y;
        points(int x, int y)
        {
            this.x=x;
            this.y=y;
        }
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.v(TAG, "init OpenCV");
        }
    }

    private PublishSubject<CameraData> subject = PublishSubject.create();

    public TextView tmer;
    public Button but;
    public int Current_stat = 0;
    public long start_time = 0;
    public long w8_time = 0;
    private Handler hndl = new Handler();
    ArrayList<points> mass = new ArrayList<points>();
    ArrayList<points> left;
    ArrayList<points> right;
    int l,r,b,t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.camera_preview);
        DrawView drawView = (DrawView) findViewById(R.id.draw_layout);
        ImageView imv = (ImageView) findViewById(R.id.imageView);
        tmer = (TextView) this.findViewById(R.id.Timer);
        but = (Button) this.findViewById(R.id.indibtn);


        cameraPreview.setCallback((data, camera) -> {
            long now = System.currentTimeMillis();
            CameraData cameraData = new CameraData();
            cameraData.data = data;
            cameraData.camera = camera;

            int frameHeight = camera.getParameters().getPreviewSize().height;
            int frameWidth = camera.getParameters().getPreviewSize().width;
            int rgb[] = new int[frameWidth * frameHeight];
            decodeYUV420SP(rgb, data, frameWidth, frameHeight);
            Bitmap bmp = Bitmap.createBitmap(rgb, frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bmp=Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

            int pixel = bmp.getPixel(bmp.getHeight()/2, bmp.getWidth()/2);

            Log.v(TAG, "____pixel rgb is: " + Color.red(pixel) + " " + Color.green(pixel) + " " + Color.blue(pixel) );

            Path path = new Path();
            int bmph = bmp.getHeight();
            int bmpw = bmp.getWidth();

            float xrat = drawView.getWidth() / (bmpw + 0.0f);
            float yrat = drawView.getHeight() / (bmph + 0.0f);
            //path.addCircle((float)(xrat*bmpw/2),(float)(yrat*bmph/2), 2.5f, Path.Direction.CCW );
            int col = 0;
            mass.clear();
            //Log.v(TAG, "____bmp size: " + bmph + " * " + bmpw + " = " + bmph * bmpw);
            // 720 * 480 = 345600



            if(Current_stat==0) {
                for (int i = 0; i < bmph; i += 10)  //72
                    for (int j = 0; j < bmpw; j += 6) { //70
                        pixel = bmp.getPixel(j, i);
                        if (Color.red(pixel) < 40 && Color.green(pixel) < 40 && Color.blue(pixel) < 40) {
                            if (check(bmp, i, j)) {
                                mass.add(new points(i / 10, j / 6));
                                Colordraw(path, i * xrat, j * yrat);
                            }
                            col++;
                        }
                    }
                if (mass.size() > 8 && mass.size() < 100) {
                    Log.v(TAG, "____mass.size(): " + mass.size());
                    if (detect(mass)&& (r-l>15)) {
                        Log.v(TAG, "____Line: " + t + " " + l + " " + b + " " + r );
                        DrawLine(path, t * 10 * xrat, l * 6 * yrat, b * 10 * xrat, r * 6 * yrat);
                        Colordraw(path, t * 10 * xrat, l * 6 * yrat);
                        Colordraw(path, t * 10 * xrat, r * 6 * yrat);
                        Colordraw(path, b * 10 * xrat, l * 6 * yrat);
                        Colordraw(path, b * 10 * xrat, r * 6 * yrat);
                        Log.v(TAG, "____now stat=1");
                        Current_stat=1;
                        w8_time=System.currentTimeMillis();
                        but.setBackground(getResources().getDrawable(R.drawable.buttonshape1));
                    }
                }
            } else if(Current_stat==1)
            {

                DrawLine(path, t * 10 * xrat, l * 6 * yrat, b * 10 * xrat, r * 6 * yrat);
                if(System.currentTimeMillis()-w8_time>2500)
                if(!checkLine(bmp))
                {
                    start_time = System.currentTimeMillis();
                    hndl.postDelayed(updTimer, 0);
                    Log.v(TAG, "____now stat=2");
                    Current_stat=2;
                    but.setBackground(getResources().getDrawable(R.drawable.buttonshape2));
                }
            } else if(Current_stat==2)
            {

                DrawLine(path, t * 10 * xrat, l * 6 * yrat, b * 10 * xrat, r * 6 * yrat);
                if(checkLine(bmp))
                {
                    Current_stat=3;
                    Log.v(TAG, "____now stat=3");
                }
            } else if(Current_stat==3)
            {
                DrawLine(path, t * 10 * xrat, l * 6 * yrat, b * 10 * xrat, r * 6 * yrat);
                if(!checkLine(bmp))
                {
                    hndl.removeCallbacks(updTimer);
                    Current_stat=4;
                    but.setBackground(getResources().getDrawable(R.drawable.buttonshape3));
                    Log.v(TAG, "____now stat=4");
                }
            }
            Log.v(TAG, "____found pix: " + col);
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            drawView.setPath(path);
            drawView.setDrawingCacheBackgroundColor(1);
            drawView.invalidate();

          //   Log.v(TAG, "____draw bitmap");
          //   imv.setImageBitmap(bmp);

            Log.v(TAG, "____one frame time:" + (System.currentTimeMillis() - now));
            subject.onNext(cameraData);
        });

        but.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (Current_stat == 1) {
                    Current_stat = 0;
                    but.setBackground(getResources().getDrawable(R.drawable.buttonshape));
                    hndl.removeCallbacks(updTimer);
                    return;
                } else if (Current_stat == 4) {
                    but.setBackground(getResources().getDrawable(R.drawable.buttonshape));
                    tmer.setText("00:00:000");
                    Current_stat = 0;
                    return;
                }
            }
        });

        cameraPreview.setOnClickListener(v -> cameraPreview.focus());
    }

    private boolean checkLine(Bitmap bmp) {
        int col = 0;
        int bl = 0;
        int pixel;
        for (int i = t * 10 + 2; i <= b * 10 - 2; i += 3)
            for (int j = l * 6 + 5; j <= r * 6 - 5; j += 2) {
                col++;
                pixel = bmp.getPixel(j, i);
                if (Color.red(pixel) < 30 && Color.green(pixel) < 30 && Color.blue(pixel) < 30)
                    bl++;
            }
        Log.v(TAG, "____line correct: " + ((bl + 0.0) / col));
        return ((bl + 0.0) / col > 0.75);
    }

    private boolean detect(ArrayList<points> mass) {
        ArrayList<ArrayList<points> > cmg = new ArrayList<ArrayList<points> >();
        ArrayList<points> left = new ArrayList<points>();
        ArrayList<points> right = new ArrayList<points>();
        boolean[] was = new boolean[(mass.size())];
        cmg.add(new ArrayList<points>());
        cmg.get(0).add(mass.get(0));
        points cur;
        for(int i=1;i<mass.size();i++)
        {
            cur=mass.get(i);
            boolean f=false;

            for(int j=0;j<cmg.size()&&!f;j++)
            {
                ArrayList<points> tmp=cmg.get(j);
                for(int k=0;k<tmp.size();k++)
                {
                    if(abs(cur.x-tmp.get(k).x)<3 && abs(cur.y-tmp.get(k).y)<3)
                    {
                        f=true;
                        cmg.get(j).add(cur);
                        break;
                    }
                }
            }

            if(!f)
            {
                cmg.add(new ArrayList<points>());
                cmg.get(cmg.size()-1).add(cur);
            }
        }

        //Log.v(TAG, "____cmg.size(): " + cmg.size());
        if(cmg.size()<2) return false;

        left=cmg.get(0);
        for(int i=1;i<cmg.size();i++)
        {
            if(cmg.get(i).size()>right.size()) {
                right = cmg.get(i);
                if(right.size()>left.size())
                {
                    ArrayList<points> tmp=right;
                    right=left;
                    left=tmp;
                }
            }
        }

        if(left.size()==0) return false;

        if(left.get(0).x>right.get(0).x)
        {
            ArrayList<points> tmp=right;
            right=left;
            left=tmp;
        }

        //Log.v(TAG, "____left.size(): " + left.size());
        //Log.v(TAG, "____right.size(): " + right.size());

        int lilt,ljlt,lh,lw;
        int rilt,rjlt,rh,rw;
        lilt=lw=left.get(0).x;
        ljlt=lh=left.get(0).y;
        rilt=rw=right.get(0).x;
        rjlt=rh=right.get(0).y;
        for(int i=0;i<left.size();i++) {
            if(lilt>left.get(i).x)
                lilt=left.get(i).x;
            if(lw<left.get(i).x)
                lw=left.get(i).x;
            if(ljlt>left.get(i).y)
                ljlt=left.get(i).y;
            if(lh<left.get(i).y)
                lh=left.get(i).y;
        }
        for(int i=0;i<right.size();i++) {
            if (rilt > right.get(i).x)
                rilt = right.get(i).x;
            if (rw < right.get(i).x)
                rw = right.get(i).x;
            if (rjlt > right.get(i).y)
                rjlt = right.get(i).y;
            if (rh < right.get(i).y)
                rh = right.get(i).y;
        }
        if(abs(lilt-rilt)<2&&abs(lw-rw)<2 && abs(lh-rjlt)>6)
        {
            l=min(ljlt, rjlt);
            t=min(lilt, rilt);;
            b=max(lw,rw);
            r=max(lh,rh);;
            return true;
        }
        return false;
    }

    private int abs(int i) {
        if(i<0) return -i;
        return i;
    }

    private Runnable updTimer = new Runnable() {
        public void run() {
            long cur = System.currentTimeMillis();
            tmer.setText(String.format("%02d:%02d:%03d",
                    ((cur - start_time) / 1000) / 60,
                    ((cur - start_time) / 1000) % 60,
                    (cur - start_time) % 1000));
            hndl.postDelayed(this, 0);
        }
    };

    static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    static public void Colordraw(Path path, double y ,double x) {
            path.addCircle((float)x,(float)y,2.5f,Path.Direction.CCW );
    }

    static public void DrawLine(Path path, double t ,double l, double b, double r) {
        path.moveTo((float)l,(float)t);
        path.lineTo((float)r,(float)t);
        path.lineTo((float)r,(float)b);
        path.lineTo((float)l,(float)b);
        path.lineTo((float)l,(float)t);
    }

    static public int min(int a, int b) {
        if(a>b) return b;
        return a;
    }

    static public int max(int a, int b) {
        if(a<b) return b;
        return a;
    }

    static public boolean check(Bitmap bmp, int y, int x) {
        int col=0;
        int num=0;
        int pixel;
        for(int i=max(0,y-30);i<=min(y+30,bmp.getHeight()-1);i+=5)
            for(int j=max(0,x-30);j<=min(x+30,bmp.getWidth()-1);j+=5) {
                pixel = bmp.getPixel(j, i);
                if (Color.red(pixel) > 150 && Color.green(pixel) > 150 && Color.blue(pixel) > 150)
                    col++;
                num++;
            }
        //if(((col+0.0)/num)>0.3)
        //Log.v(TAG, "____value:" + ((col+0.0)/num));
        return ((col+0.0)/num)>0.40;
        //return true;
    }

}
