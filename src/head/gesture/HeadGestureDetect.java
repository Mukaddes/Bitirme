package head.gesture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.Video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;

public class HeadGestureDetect extends Activity implements CvCameraViewListener {
	
	private ArrayList<HeadGestureListener> listeners = new ArrayList<HeadGestureListener>();
	
	private static PriorityQueue<Direction> pq = new PriorityQueue<Direction>();
	
	private  Direction up = new Direction("up",0);
	private  Direction down = new Direction("down",0);
	private  Direction left = new Direction("left",0);
	private  Direction right = new Direction("right",0);
	
	private final double NOISE = (float) 20.0;

	public static final int VIEW_MODE_RGBA = 0;
	public static final int VIEW_MODE_HOUGHCIRCLES = 1;
	public static final int VIEW_MODE_HOUGHLINES = 2;
	public static final int VIEW_MODE_CANNY = 3;
	public static final int VIEW_MODE_COLCONTOUR = 4;
	public static final int VIEW_MODE_FACEDETECT = 5;
	public static final int VIEW_MODE_YELLOW_QUAD_DETECT = 6;
	public static final int VIEW_MODE_GFTT = 7;
	public static final int VIEW_MODE_OPFLOW = 8;

	public static int viewMode = VIEW_MODE_RGBA;

	private CascadeClassifier mCascade;

	private boolean bShootNow = false, bDisplayTitle = true,
			bFirstFaceSaved = false;

	private byte[] byteColourTrackCentreHue;

	private double d, dTextScaleFactor, x1, x2, y1, y2;

	private double[] vecHoughLines;

	private Point pt, pt1, pt2;

	private int x, y, radius, iMinRadius, iMaxRadius, iCannyLowerThreshold,
			iCannyUpperThreshold, iAccumulator, iLineThickness = 3,
			iHoughLinesThreshold = 50, iHoughLinesMinLineSize = 20,
			iHoughLinesGap = 20, iMaxFaceHeight, iMaxFaceHeightIndex,
			iFileOrdinal = 0, iCamera = 0, iNumberOfCameras = 0, iGFFTMax = 20,
			iContourAreaMin = 1000;

	private JavaCameraView mOpenCvCameraView0;
	private JavaCameraView mOpenCvCameraView1;

	private List<Byte> byteStatus;
	private List<Integer> iHueMap, channels;
	private List<Float> ranges;
	private List<Point> pts, corners, cornersThis, cornersPrev;
	private List<MatOfPoint> contours;
	private ArrayList<String> testFileList;

	
	private long lFrameCount = 0, lMilliStart = 0, lMilliNow = 0,
			lMilliShotTime = 0;

	private Mat mRgba, mGray, mIntermediateMat, mMatRed, mMatGreen, mMatBlue,
			mROIMat, mMatRedInv, mMatGreenInv, mMatBlueInv, mHSVMat,
			mErodeKernel, mContours, lines, mFaceDest, mFaceResized,
			matOpFlowPrev, matOpFlowThis, matFaceHistogramPrevious,
			matFaceHistogramThis, mHist;

	private MatOfFloat mMOFerr, MOFrange;
	private MatOfRect faces;
	private MatOfByte mMOBStatus;
	private MatOfPoint2f mMOP2f1, mMOP2f2, mMOP2fptsPrev, mMOP2fptsThis,
			mMOP2fptsSafe;
	private MatOfPoint2f mApproxContour;
	private MatOfPoint MOPcorners;
	private MatOfInt MOIone, histSize;

	private Rect rect, rDest;

	private Scalar colorRed, colorGreen;
	private Size sSize, sSize3, sSize5, sMatSize;
	private String string, sShotText;
	
	private File path;
	private FileOutputStream fout;
	private PrintStream ps ;
	private Context context;
	private AlertDialog.Builder dialog;
	
	
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:

//							if (iNumberOfCameras > 1)
//								mOpenCvCameraView1.enableView();
							
				        	runFromCamera();
				            break;

				        case DialogInterface.BUTTON_NEGATIVE:
				        	onCameraViewStarted(480,720);
				            runTest();
				            break;
				        }
				        
				        dialog.dismiss();
				    }
				};

				dialog = new AlertDialog.Builder(context);
				dialog.setMessage("Please select").setPositiveButton("Run from camera", dialogClickListener)
				    .setNegativeButton("Run test", dialogClickListener).show();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
	
	public void setOnHeadGestureDetectedListener(HeadGestureListener listener){
		listeners.add(listener);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		iNumberOfCameras = Camera.getNumberOfCameras();
		
		path = new File(Environment.getExternalStorageDirectory().getPath() + "/head.txt");
		try {
			fout = new FileOutputStream(path);
			ps = new PrintStream(fout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.head_gesture_detect);

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this,
				mLoaderCallback);
	}
	private void runTest(){
		try {
			onDirectoryFrame();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void runFromCamera(){
		mOpenCvCameraView0 = (JavaCameraView) findViewById(R.id.java_surface_view0);
		mOpenCvCameraView0.enableView();
		//mOpenCvCameraView0.setRotation(90);

		//if (iNumberOfCameras > 1)
			//mOpenCvCameraView1 = (JavaCameraView) findViewById(R.id.java_surface_view1);

		mOpenCvCameraView0.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView0.setCvCameraViewListener(this);

		mOpenCvCameraView0.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

//		if (iNumberOfCameras > 1) {
//			mOpenCvCameraView1.setVisibility(SurfaceView.GONE);
//			mOpenCvCameraView1.setCvCameraViewListener(this);
//			mOpenCvCameraView1.setLayoutParams(new LayoutParams(
//					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView0 != null)
			mOpenCvCameraView0.disableView();
		if (iNumberOfCameras > 1)
			if (mOpenCvCameraView1 != null)
				mOpenCvCameraView1.disableView();
	}

	public void onResume() {
		super.onResume();

		viewMode = VIEW_MODE_RGBA;

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView0 != null)
			mOpenCvCameraView0.disableView();
		if (iNumberOfCameras > 1)
			if (mOpenCvCameraView1 != null)
				mOpenCvCameraView1.disableView();
		try {
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.opencvd2, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		viewMode = VIEW_MODE_OPFLOW;
		lFrameCount = 0;
		lMilliStart = 0;

		return true;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		byteColourTrackCentreHue = new byte[3];
		// green = 60 // mid yellow 27
		byteColourTrackCentreHue[0] = 27;
		byteColourTrackCentreHue[1] = 100;
		byteColourTrackCentreHue[2] = (byte) 255;
		byteStatus = new ArrayList<Byte>();

		channels = new ArrayList<Integer>();
		channels.add(0);
		colorRed = new Scalar(255, 0, 0, 255);
		colorGreen = new Scalar(0, 255, 0, 255);
		contours = new ArrayList<MatOfPoint>();
		corners = new ArrayList<Point>();
		cornersThis = new ArrayList<Point>();
		cornersPrev = new ArrayList<Point>();

		faces = new MatOfRect();

		histSize = new MatOfInt(25);

		iHueMap = new ArrayList<Integer>();
		iHueMap.add(0);
		iHueMap.add(0);
		lines = new Mat();

		mApproxContour = new MatOfPoint2f();
		mContours = new Mat();
		mHist = new Mat();
		mGray = new Mat();
		mHSVMat = new Mat();
		mIntermediateMat = new Mat();
		mMatRed = new Mat();
		mMatGreen = new Mat();
		mMatBlue = new Mat();
		mMatRedInv = new Mat();
		mMatGreenInv = new Mat();
		mMatBlueInv = new Mat();
		MOIone = new MatOfInt(0);

		MOFrange = new MatOfFloat(0f, 256f);
		mMOP2f1 = new MatOfPoint2f();
		mMOP2f2 = new MatOfPoint2f();
		mMOP2fptsPrev = new MatOfPoint2f();
		mMOP2fptsThis = new MatOfPoint2f();
		mMOP2fptsSafe = new MatOfPoint2f();
		mMOFerr = new MatOfFloat();
		mMOBStatus = new MatOfByte();
		MOPcorners = new MatOfPoint();
		mRgba = new Mat();
		mROIMat = new Mat();
		mFaceDest = new Mat();
		mFaceResized = new Mat();
		matFaceHistogramPrevious = new Mat();
		matFaceHistogramThis = new Mat();
		matOpFlowThis = new Mat();
		matOpFlowPrev = new Mat();
		testFileList = new ArrayList<String>();

		pt = new Point(0, 0);
		pt1 = new Point(0, 0);
		pt2 = new Point(0, 0);

		pts = new ArrayList<Point>();

		ranges = new ArrayList<Float>();
		ranges.add(50.0f);
		ranges.add(256.0f);
		rect = new Rect();
		rDest = new Rect();

		sMatSize = new Size();
		sSize = new Size();
		sSize3 = new Size(3, 3);
		sSize5 = new Size(5, 5);

		string = "";

		DisplayMetrics dm = this.getResources().getDisplayMetrics();
		int densityDpi = dm.densityDpi;
		dTextScaleFactor = ((double) densityDpi / 240.0) * 0.9;

		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
	}

	@Override
	public void onCameraViewStopped() {
		releaseMats();
	}

	public void releaseMats() {
		mRgba.release();
		mIntermediateMat.release();
		mGray.release();
		mMatRed.release();
		mMatGreen.release();
		mMatBlue.release();
		mROIMat.release();
		mMatRedInv.release();
		mMatGreenInv.release();
		mMatBlueInv.release();
		mHSVMat.release();
		mErodeKernel.release();
		mContours.release();
		lines.release();
		faces.release();
		MOPcorners.release();
		mMOP2f1.release();
		mMOP2f2.release();
		mApproxContour.release();

	}
	
	static int frame_number = 0;
	
	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		
		up.value = 0;
		down.value = 0;
		left.value = 0;
		right.value = 0;
		pq.clear();
		
		// start the timing counter to put the frame rate on screen
		// and make sure the start time is up to date, do
		// a reset every 10 seconds
		if (lMilliStart == 0)
			lMilliStart = System.currentTimeMillis();

		if ((lMilliNow - lMilliStart) > 10000) {
			lMilliStart = System.currentTimeMillis();
			lFrameCount = 0;
		}
		
		inputFrame.copyTo(mRgba);
		sMatSize.width = mRgba.width();
		sMatSize.height = mRgba.height();

		switch (viewMode) {

		case VIEW_MODE_OPFLOW:

			if (mMOP2fptsPrev.rows() == 0) {

				// Log.d("Baz", "First time opflow");
				// first time through the loop so we need prev and this mats
				// plus prev points
				// get this mat
				Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

				// copy that to prev mat
				matOpFlowThis.copyTo(matOpFlowPrev);

				// get prev corners
				Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax, 0.05, 20);
				Mat mask = new Mat();
				int blockSize = 3;
				boolean useHarrisDetector = true;
				double k = 0.04;
				//Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax, 0.05, 20,  mask, blockSize, useHarrisDetector, k);
				mMOP2fptsPrev.fromArray(MOPcorners.toArray());

				// get safe copy of this corners
				mMOP2fptsPrev.copyTo(mMOP2fptsSafe);
			} else {
				// Log.d("Baz", "Opflow");
				// we've been through before so
				// this mat is valid. Copy it to prev mat
				matOpFlowThis.copyTo(matOpFlowPrev);

				// get this mat
				Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

				// get the corners for this mat
				Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, iGFFTMax, 0.05, 20);
				mMOP2fptsThis.fromArray(MOPcorners.toArray());

				// retrieve the corners from the prev mat
				// (saves calculating them again)
				mMOP2fptsSafe.copyTo(mMOP2fptsPrev);

				// and save this corners for next time through
				mMOP2fptsThis.copyTo(mMOP2fptsSafe);
			}

			Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);

			cornersPrev = mMOP2fptsPrev.toList();
			cornersThis = mMOP2fptsThis.toList();
			/*Integer cornerCount = cornersThis.size();

			try {
				ps.println("Harris corners count = " + cornerCount.toString() );
				fout.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
			byteStatus = mMOBStatus.toList();
 
			y = byteStatus.size() - 1;

			// burada bütün featurelarýn x
			for (x = 0; x < y; x++) {
				if (byteStatus.get(x) == 1) {
					pt = cornersThis.get(x);
					pt2 = cornersPrev.get(x);
					
					double distance= Math.sqrt(Math.pow((pt.x - pt2.x),2) + Math.pow((pt.y - pt2.y),2));
					
					if(distance < NOISE) {
						string = String.format("Direction: ---");
						showTitle(string, 3, colorRed);
						continue;
					}
					
//					try {
//						ps.printf("Distance = %f\n", distance );
//						fout.flush();
//					} catch (FileNotFoundException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
					
					double m = Math.abs(pt2.y - pt.y ) / Math.abs(pt2.x - pt.x);
					
					if (pt.x < pt2.x && pt2.y < pt.y)

						if (m > 1)
							up.value++;
						else
							right.value++;

					else if (pt.x < pt2.x && pt2.y == pt.y)
						right.value++;

					else if (pt.x < pt2.x && pt2.y > pt.y)
						if (m > 1)
							down.value++;
						else
							right.value++;

					else if (pt.x == pt2.x && pt2.y > pt.y)
						down.value++;

					else if (pt.x > pt2.x && pt2.y > pt.y)
						if (m > 1)
							down.value++;
						else
							left.value++;

					else if (pt.x > pt2.x && pt2.y == pt.y)
						left.value++;

					else if (pt.x > pt2.x && pt2.y < pt.y)
						if (m > 1)
							up.value++;
						else
							left.value++;

					else if (pt.x == pt2.x && pt2.y < pt.y)
						up.value++;
					
					Core.circle(mRgba, pt, 5, colorRed, iLineThickness - 1);
					Core.line(mRgba, pt, pt2, colorRed, iLineThickness);
				}
			}//end of for
			
			Direction r1, r2, r3;
			
			if(up.value == 0 && left.value == 0 && right.value == 0 && down.value == 0) {
				string = String.format("Direction: ---");
				showTitle(string, 3, colorRed);
				
			}else{
			
				if (left.value < right.value) {
					r1 = left;
				} else r1 = right;
				
				if (up.value < down.value) {
					r2 = up;
				} else r2 = down;
				
				if (r1.value < r2.value) {
					r3 = r2;
				} else r3 = r1;
				
				string = String.format("Direction: %s", r3.name);
				
				for (HeadGestureListener listener : listeners) {
				    listener.onHeadGestureDetected(r3.name);
				}
				
				showTitle(string, 3, colorRed);
			}
			
			//Log.d("Mukcay",pq.poll().name );
			// Log.d("Baz", "Opflow feature count: "+x);
			if (bDisplayTitle)
				showTitle("Optical Flow", 1, colorGreen);
				break;
		}

		// get the time now in every frame
		lMilliNow = System.currentTimeMillis();

		// update the frame counter
		lFrameCount++;

		if (bDisplayTitle) {
			string = String.format("FPS: %2.1f", (float) (lFrameCount * 1000) / (float) (lMilliNow - lMilliStart));
			showTitle(string, 2, colorGreen);
		}

		if (System.currentTimeMillis() - lMilliShotTime < 1500)
			showTitle(sShotText, 3, colorRed);
		
		File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath()+"/output");
		
		if (!folder.exists()) {
			Log.d("MUKCAY", "Dizin yok!");
			folder.mkdirs();
		}
		
		String pathName = folder+"/picture_"+frame_number++ + ".jpeg";
		
		Bitmap b = Bitmap.createBitmap(inputFrame.width(), inputFrame.height() ,Config.ARGB_8888);
		Utils.matToBitmap(mRgba, b);
		
		File image = new File(pathName);
		
	    try {
	        image.createNewFile();
	        // BufferedOutputStream os = new BufferedOutputStream(
	        // new FileOutputStream(file));

	        FileOutputStream os = new FileOutputStream(image);
	        b.compress(Bitmap.CompressFormat.JPEG, 100, os);
	        os.flush();
	        os.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		return mRgba;
	}

	public boolean onTouchEvent(final MotionEvent event) {

		bShootNow = true;
		return false; // don't need more than one touch event

	}

	public void DrawCross(Mat mat, Scalar color, Point pt) {
		int iCentreCrossWidth = 24;

		pt1.x = pt.x - (iCentreCrossWidth >> 1);
		pt1.y = pt.y;
		pt2.x = pt.x + (iCentreCrossWidth >> 1);
		pt2.y = pt.y;

		Core.line(mat, pt1, pt2, color, iLineThickness - 1);

		pt1.x = pt.x;
		pt1.y = pt.y + (iCentreCrossWidth >> 1);
		pt2.x = pt.x;
		pt2.y = pt.y - (iCentreCrossWidth >> 1);

		Core.line(mat, pt1, pt2, color, iLineThickness - 1);

	}

	public Mat getHistogram(Mat mat) {
		Imgproc.calcHist(Arrays.asList(mat), MOIone, new Mat(), mHist,
				histSize, MOFrange);

		Core.normalize(mHist, mHist);

		return mHist;
	}

	@SuppressLint("SimpleDateFormat")
	public boolean SaveImage(Mat mat, String filename) {

		Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

		File path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

		//String filename = "OpenCV_";
		//SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		//Date date = new Date(System.currentTimeMillis());
		//String dateString = fmt.format(date);
		//filename += dateString + "-" + iFileOrdinal;
		//filename += ".png";

		//File file = new File(path, filename);

		Boolean bool = null;
		//filename = file.toString();
		bool = Highgui.imwrite(filename, mIntermediateMat);

		// if (bool == false)
		// Log.d("Baz", "Fail writing image to external storage");

		return bool;
	}
	
	private void showTitle(String s, int iLineNum, Scalar color) {
		Core.putText(mRgba, s, new Point(10, (int) (dTextScaleFactor * 60 * iLineNum)), Core.FONT_HERSHEY_SIMPLEX, dTextScaleFactor, color, 2);
	}
	public void onDirectoryFrame() throws IOException {

		File folder = 
				new File(Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_DOCUMENTS).getPath()+"/frames/right/1");
		
		if (!folder.exists()) {
			Log.d("MUKCAY", "Dizin yok!");
			folder.mkdirs();
			return;
		}
		listFilesForFolder(folder);
		
		//Log.d("MUKCAY", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath()+"/frames/right/1" );
		File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath()+"/output");
		
		if (!f.exists()) {
			Log.d("MUKCAY", "Dizin yok!");
			f.mkdirs();
		}
		
		
		Mat img = Highgui.imread(folder+"/"+testFileList.get(0));
		onTestFrame(img, testFileList.get(0) );
		
		for (int i = 1; i < testFileList.size(); i++) {
			img = Highgui.imread(folder+"/"+testFileList.get(i));
			
			//String file = f+"/"+testFileList.get(i);
			//Highgui.imwrite(file, mRgba);  // write to disk
			
			onTestFrame(img, testFileList.get(i) );
		}
	}

	public void listFilesForFolder(final File folder) {
		
		if(folder.exists() && folder.isDirectory()) {
			File[] file_array = folder.listFiles();
			
			if (file_array == null) {
				Log.d("MUKCAY", "Dosya dizisi null!");
				return;
			}
			
			for (final File fileEntry : file_array) {
				if (fileEntry.isDirectory()) {
					listFilesForFolder(fileEntry);
				} else {
					testFileList.add(fileEntry.getName());
				}
			}
		}
		else {
			Log.d("MUKCAY", "Bu bir dosya, dizin deðil!");
			Log.d("MUKCAY", folder.getAbsolutePath());
		}
	}

	public Mat onTestFrame(Mat inputFrame, String filename) throws IOException {
		up.value = 0;
		down.value = 0;
		left.value = 0;
		right.value = 0;
		pq.clear();

		// start the timing counter to put the frame rate on screen
		// and make sure the start time is up to date, do
		// a reset every 10 seconds
		if (lMilliStart == 0)
			lMilliStart = System.currentTimeMillis();

		if ((lMilliNow - lMilliStart) > 10000) {
			lMilliStart = System.currentTimeMillis();
			lFrameCount = 0;
		}

		inputFrame.copyTo(mRgba);
		sMatSize.width = mRgba.width();
		sMatSize.height = mRgba.height();

		if (mMOP2fptsPrev.rows() == 0) {

			// Log.d("Baz", "First time opflow");
			// first time through the loop so we need prev and this mats
			// plus prev points
			// get this mat
			Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

			// copy that to prev mat
			matOpFlowThis.copyTo(matOpFlowPrev);

			// get prev corners
			Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax,
					0.05, 20);
			Mat mask = new Mat();
			int blockSize = 3;
			boolean useHarrisDetector = true;
			double k = 0.04;
			// Imgproc.goodFeaturesToTrack(matOpFlowPrev, MOPcorners, iGFFTMax,
			// 0.05, 20, mask, blockSize, useHarrisDetector, k);
			mMOP2fptsPrev.fromArray(MOPcorners.toArray());

			// get safe copy of this corners
			mMOP2fptsPrev.copyTo(mMOP2fptsSafe);
		} else {
			// Log.d("Baz", "Opflow");
			// we've been through before so
			// this mat is valid. Copy it to prev mat
			matOpFlowThis.copyTo(matOpFlowPrev);

			// get this mat
			Imgproc.cvtColor(mRgba, matOpFlowThis, Imgproc.COLOR_RGBA2GRAY);

			// get the corners for this mat
			Imgproc.goodFeaturesToTrack(matOpFlowThis, MOPcorners, iGFFTMax,
					0.05, 20);
			mMOP2fptsThis.fromArray(MOPcorners.toArray());

			// retrieve the corners from the prev mat
			// (saves calculating them again)
			mMOP2fptsSafe.copyTo(mMOP2fptsPrev);

			// and save this corners for next time through
			mMOP2fptsThis.copyTo(mMOP2fptsSafe);
		}

		Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev,
				mMOP2fptsThis, mMOBStatus, mMOFerr);

		cornersPrev = mMOP2fptsPrev.toList();
		cornersThis = mMOP2fptsThis.toList();
		/*
		 * Integer cornerCount = cornersThis.size();
		 * 
		 * try { ps.println("Harris corners count = " + cornerCount.toString()
		 * ); fout.flush(); } catch (FileNotFoundException e) {
		 * e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
		 */
		byteStatus = mMOBStatus.toList();

		y = byteStatus.size() - 1;

		// burada bütün featurelarýn x
		for (x = 0; x < y; x++) {
			if (byteStatus.get(x) == 1) {
				pt = cornersThis.get(x);
				pt2 = cornersPrev.get(x);

				double distance = Math.sqrt(Math.pow((pt.x - pt2.x), 2)
						+ Math.pow((pt.y - pt2.y), 2));

				if (distance < NOISE) {
					string = String.format("Direction: ---");
					showTitle(string, 3, colorRed);
					continue;
				}

				// try {
				// ps.printf("Distance = %f\n", distance );
				// fout.flush();
				// } catch (FileNotFoundException e) {
				// e.printStackTrace();
				// } catch (IOException e) {
				// e.printStackTrace();
				// }

				double m = Math.abs(pt2.y - pt.y) / Math.abs(pt2.x - pt.x);

				if (pt.x < pt2.x && pt2.y < pt.y)

					if (m > 1)
						up.value++;
					else
						right.value++;

				else if (pt.x < pt2.x && pt2.y == pt.y)
					right.value++;

				else if (pt.x < pt2.x && pt2.y > pt.y)
					if (m > 1)
						down.value++;
					else
						right.value++;

				else if (pt.x == pt2.x && pt2.y > pt.y)
					down.value++;

				else if (pt.x > pt2.x && pt2.y > pt.y)
					if (m > 1)
						down.value++;
					else
						left.value++;

				else if (pt.x > pt2.x && pt2.y == pt.y)
					left.value++;

				else if (pt.x > pt2.x && pt2.y < pt.y)
					if (m > 1)
						up.value++;
					else
						left.value++;

				else if (pt.x == pt2.x && pt2.y < pt.y)
					up.value++;

				Core.circle(mRgba, pt, 5, colorRed, iLineThickness - 1);
				Core.line(mRgba, pt, pt2, colorRed, iLineThickness);
			}
		}// end of for

		Direction r1, r2, r3;

		if (up.value == 0 && left.value == 0 && right.value == 0
				&& down.value == 0) {
			string = String.format("Direction: ---");
			showTitle(string, 3, colorRed);

		} else {

			if (left.value < right.value) {
				r1 = left;
			} else
				r1 = right;

			if (up.value < down.value) {
				r2 = up;
			} else
				r2 = down;

			if (r1.value < r2.value) {
				r3 = r2;
			} else
				r3 = r1;

			string = String.format("Direction: %s", r3.name);

			for (HeadGestureListener listener : listeners) {
				listener.onHeadGestureDetected(r3.name);
			}

			showTitle(string, 3, colorRed);
		}

		// Log.d("Mukcay",pq.poll().name );
		// Log.d("Baz", "Opflow feature count: "+x);
		if (bDisplayTitle)
			showTitle("Optical Flow", 1, colorGreen);


		// get the time now in every frame
		lMilliNow = System.currentTimeMillis();

		// update the frame counter
		lFrameCount++;

		if (bDisplayTitle) {
			string = String.format("FPS: %2.1f", (float) (lFrameCount * 1000)
					/ (float) (lMilliNow - lMilliStart));
			showTitle(string, 2, colorGreen);
		}

		if (System.currentTimeMillis() - lMilliShotTime < 1500)
			showTitle(sShotText, 3, colorRed);

		File folder = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOCUMENTS).getPath()
				+ "/output");

		if (!folder.exists()) {
			Log.d("MUKCAY", "Dizin yok!");
			folder.mkdirs();
		}

		String pathName = folder + "/" + filename;

		Bitmap b = Bitmap.createBitmap(inputFrame.width(), inputFrame.height(),
				Config.ARGB_8888);
		Utils.matToBitmap(mRgba, b);

		File image = new File(pathName);

		try {
			image.createNewFile();
			// BufferedOutputStream os = new BufferedOutputStream(
			// new FileOutputStream(file));

			FileOutputStream os = new FileOutputStream(image);
			b.compress(Bitmap.CompressFormat.JPEG, 100, os);
			os.flush();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mRgba;

		// SaveImage(mRgba, pathName);

		// Bitmap b = Bitmap.createBitmap(inputFrame.width(),
		// inputFrame.height() ,Config.ARGB_8888);
		// Utils.matToBitmap(mRgba, b);
		//
		//
		// File image = new File(pathName);
		// try {
		// image.createNewFile();
		// // BufferedOutputStream os = new BufferedOutputStream(
		// // new FileOutputStream(file));
		//
		// FileOutputStream os = new FileOutputStream(image);
		// b.compress(Bitmap.CompressFormat.JPEG, 100, os);
		// os.flush();
		// os.close();
		//
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		// Highgui.imwrite(file, mRgba); // write to disk

		// update the frame counter

	}
}
