package head.gesture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.opencv.utils.Converters;
import org.opencv.video.Video;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class HeadGestureDetect extends Fragment implements CvCameraViewListener {
	
	private ArrayList<HeadGestureListener> listeners = new ArrayList<HeadGestureListener>();
	
	private static PriorityQueue<Direction> pq = new PriorityQueue<Direction>();
	
	private  Direction up = new Direction("up",0);
	private  Direction down = new Direction("down",0);
	private  Direction left = new Direction("left",0);
	private  Direction right = new Direction("right",0);
	
	private final double NOISE = (float) 3.0;

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
			iFileOrdinal = 0, iCamera = 0, iNumberOfCameras = 0, iGFFTMax = 40,
			iContourAreaMin = 1000;

	private JavaCameraView mOpenCvCameraView0;
	private JavaCameraView mOpenCvCameraView1;

	private List<Byte> byteStatus;
	private List<Integer> iHueMap, channels;
	private List<Float> ranges;
	private List<Point> pts, corners, cornersThis, cornersPrev;
	private List<MatOfPoint> contours;

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



	public void setOnHeadGestureDetectedListener(HeadGestureListener listener){
		listeners.add(listener);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		iNumberOfCameras = Camera.getNumberOfCameras();

		// Log.d(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		//setContentView(R.layout.head_gesture_detect);

		mOpenCvCameraView0 = (JavaCameraView) getView().findViewById(R.id.java_surface_view0);

		if (iNumberOfCameras > 1)
			mOpenCvCameraView1 = (JavaCameraView) getView().findViewById(R.id.java_surface_view1);

		mOpenCvCameraView0.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView0.setCvCameraViewListener(this);

		mOpenCvCameraView0.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		if (iNumberOfCameras > 1) {
			mOpenCvCameraView1.setVisibility(SurfaceView.GONE);
			mOpenCvCameraView1.setCvCameraViewListener(this);
			mOpenCvCameraView1.setLayoutParams(new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
	        return inflater.inflate(R.layout.head_gesture_detect, container, false);
	    }
	
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//		iNumberOfCameras = Camera.getNumberOfCameras();
//
//		// Log.d(TAG, "called onCreate");
//		super.onCreate(savedInstanceState);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//		setContentView(R.layout.head_gesture_detect);
//
//		mOpenCvCameraView0 = (JavaCameraView) findViewById(R.id.java_surface_view0);
//
//		if (iNumberOfCameras > 1)
//			mOpenCvCameraView1 = (JavaCameraView) findViewById(R.id.java_surface_view1);
//
//		mOpenCvCameraView0.setVisibility(SurfaceView.VISIBLE);
//		mOpenCvCameraView0.setCvCameraViewListener(this);
//
//		mOpenCvCameraView0.setLayoutParams(new LayoutParams(
//				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//
//		if (iNumberOfCameras > 1) {
//			mOpenCvCameraView1.setVisibility(SurfaceView.GONE);
//			mOpenCvCameraView1.setCvCameraViewListener(this);
//			mOpenCvCameraView1.setLayoutParams(new LayoutParams(
//					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//		}
//
//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
//				mLoaderCallback);
//	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView0 != null)
			mOpenCvCameraView0.disableView();
		if (iNumberOfCameras > 1)
			if (mOpenCvCameraView1 != null)
				mOpenCvCameraView1.disableView();
	}

//	public void onResume() {
//		super.onResume();
//
//		viewMode = VIEW_MODE_RGBA;
//
//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
//				mLoaderCallback);
//	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView0 != null)
			mOpenCvCameraView0.disableView();
		if (iNumberOfCameras > 1)
			if (mOpenCvCameraView1 != null)
				mOpenCvCameraView1.disableView();
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.opencvd2, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		viewMode = VIEW_MODE_OPFLOW;
//		lFrameCount = 0;
//		lMilliStart = 0;
//
//		return true;
//	}

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

			/*
			 * Parameters: prevImg first 8-bit input image nextImg second input
			 * image prevPts vector of 2D points for which the flow needs to be
			 * found; point coordinates must be single-precision floating-point
			 * numbers. nextPts output vector of 2D points (with
			 * single-precision floating-point coordinates) containing the
			 * calculated new positions of input features in the second image;
			 * when OPTFLOW_USE_INITIAL_FLOW flag is passed, the vector must
			 * have the same size as in the input. status output status vector
			 * (of unsigned chars); each element of the vector is set to 1 if
			 * the flow for the corresponding features has been found,
			 * otherwise, it is set to 0. err output vector of errors; each
			 * element of the vector is set to an error for the corresponding
			 * feature, type of the error measure can be set in flags parameter;
			 * if the flow wasn't found then the error is not defined (use the
			 * status parameter to find such cases).
			 */
			Video.calcOpticalFlowPyrLK(matOpFlowPrev, matOpFlowThis, mMOP2fptsPrev, mMOP2fptsThis, mMOBStatus, mMOFerr);

			cornersPrev = mMOP2fptsPrev.toList();
			cornersThis = mMOP2fptsThis.toList();
			byteStatus = mMOBStatus.toList();
 
			y = byteStatus.size() - 1;

			// burada bütün featurelarýn x
			for (x = 0; x < y; x++) {
				if (byteStatus.get(x) == 1) {
					pt = cornersThis.get(x);
					pt2 = cornersPrev.get(x);
					double m = Math.abs(pt2.y - pt.y ) / Math.abs(pt2.x - pt.x);
					
					double distance= Math.sqrt(Math.pow((pt.x - pt2.x),2) + Math.pow((pt.y - pt2.y),2));
					//Log.d("Mukcay","distance = " + distance );
					
					if(distance < NOISE)
						continue;
					
					/*if( m == 1.0 ){
						Log.d("Mukcay","YON = KD" );
					} else if( m < 1.0 ) {
						Log.d("Mukcay","YON = SAG" );
					} else Log.d("Mukcay","YON = YUKARI" );*/
					
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
					r1 = right;
				} else r1 = left;
				
				if (up.value < down.value) {
					r2 = down;
				} else r2 = up;
				
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
	public boolean SaveImage(Mat mat) {

		Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

		File path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

		String filename = "OpenCV_";
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date(System.currentTimeMillis());
		String dateString = fmt.format(date);
		filename += dateString + "-" + iFileOrdinal;
		filename += ".png";

		File file = new File(path, filename);

		Boolean bool = null;
		filename = file.toString();
		bool = Highgui.imwrite(filename, mIntermediateMat);

		// if (bool == false)
		// Log.d("Baz", "Fail writing image to external storage");

		return bool;

	}
	
	private void showTitle(String s, int iLineNum, Scalar color) {
		Core.putText(mRgba, s, new Point(10, (int) (dTextScaleFactor * 60 * iLineNum)), Core.FONT_HERSHEY_SIMPLEX, dTextScaleFactor, color, 2);
	}
}
