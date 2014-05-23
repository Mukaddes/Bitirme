package head.gesture;

import head.gesture.SimpleGestureFilter.SimpleGestureListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class SwipeScreenExample extends FragmentActivity implements SimpleGestureListener {
	private JavaCameraView mOpenCvCameraView0;
	private JavaCameraView mOpenCvCameraView1;
	private CascadeClassifier mCascade;
	private int iNumberOfCameras = 0;
	
	public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				mOpenCvCameraView0.enableView();

				if (iNumberOfCameras > 1)
					mOpenCvCameraView1.enableView();

				try {
					// DO FACE CASCADE SETUP

					Context context = getApplicationContext();
					InputStream is3 = context.getResources().openRawResource(
							R.raw.haarcascade_frontalface_default);
					File cascadeDir = context.getDir("cascade",
							Context.MODE_PRIVATE);
					File cascadeFile = new File(cascadeDir,
							"haarcascade_frontalface_default.xml");

					FileOutputStream os = new FileOutputStream(cascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;

					while ((bytesRead = is3.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}

					is3.close();
					os.close();

					mCascade = new CascadeClassifier(
							cascadeFile.getAbsolutePath());

					if (mCascade.empty()) {
						// Log.d(TAG, "Failed to load cascade classifier");
						mCascade = null;
					}

					cascadeFile.delete();
					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
					// Log.d(TAG, "Failed to load cascade. Exception thrown: " +
					// e);
				}

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	private SimpleGestureFilter detector;
	ArrayList<Questions> questions = new ArrayList<Questions>();
	Integer score = 0;
	Character answer;
	int nextQ;
	private Chronometer ch;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.swipe_screen);
		generateQuestions();
		
		detector = new SimpleGestureFilter(this, this);
		
		//ilk soruyu sor
		nextQ = 0;
		TextView q = (TextView)findViewById(R.id.question);
		q.setText( questions.get(nextQ).getQuestion());
		TextView a1 = (TextView)findViewById(R.id.answer1);
		a1.setText("a. " +questions.get(nextQ).getA());
		TextView a2 = (TextView)findViewById(R.id.answer2);
		a2.setText("b. " + questions.get(nextQ).getB());
		TextView a3 = (TextView)findViewById(R.id.answer3);
		a3.setText("c. " +questions.get(nextQ).getC());
		TextView a4 = (TextView)findViewById(R.id.answer4);
		a4.setText("d. " +questions.get(nextQ).getD());
		ch = (Chronometer) findViewById(R.id.chronometer1);
		ch.setBase(SystemClock.elapsedRealtime());
        ch.start();
	
	}

	
	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		// Call onTouchEvent of SimpleGestureFilter class
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	@Override
	public void onSwipe(int direction) {
		String str = "";
		 
         ch.stop();
		switch (direction) {

		case SimpleGestureFilter.SWIPE_RIGHT:
			answer = 'b';
			str = "Swipe Right";
			break;
		case SimpleGestureFilter.SWIPE_LEFT:
			answer = 'd';
			str = "Swipe Left";
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
			answer = 'c';
			str = "Swipe Down";
			break;
		case SimpleGestureFilter.SWIPE_UP:
			answer = 'a';
			str = "Swipe Up";
			break;

		}
		
		if(answer == questions.get(nextQ).getRightAnswer())
		{
			score = score + 100;
			new AlertDialog.Builder(this).setTitle("Doðru").setMessage(score.toString()+" Puan!").setIcon(R.drawable.ok).setNeutralButton("Kapat", null).show();
			
		}
		else 
			new AlertDialog.Builder(this).setTitle("Üzgünüm").setMessage("Yanlýþ!").setIcon(R.drawable.wrong).setNeutralButton("Kapat", null).show();
		
		TextView tw = (TextView) findViewById(R.id.direction);
		tw.setText(str);
		
		
		

		nextQ ++;
		if(nextQ == questions.size()){
			new AlertDialog.Builder(this).setTitle("Oyun bitti").setMessage("Toplam puanýnýz: " + score.toString()).setNeutralButton("Kapat", null).show();
			/*try {
	            Thread.sleep(1000);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }*/
			Intent in = new Intent(getApplicationContext(), StartUpScreen.class);
			startActivity(in);
			return;
		}
		TextView q = (TextView)findViewById(R.id.question);
		q.setText( questions.get(nextQ).getQuestion());
		TextView a1 = (TextView)findViewById(R.id.answer1);
		a1.setText("a. " +questions.get(nextQ).getA());
		TextView a2 = (TextView)findViewById(R.id.answer2);
		a2.setText("b. " + questions.get(nextQ).getB());
		TextView a3 = (TextView)findViewById(R.id.answer3);
		a3.setText("c. " +questions.get(nextQ).getC());
		TextView a4 = (TextView)findViewById(R.id.answer4);
		a4.setText("d. " +questions.get(nextQ).getD());
			
	}

	@Override
	public void onDoubleTap() {
		Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
	}
	private void generateQuestions() {
		
		Questions q1 = new Questions("Türk sinemasýnýn sultan lakabý ile anýlan aktiristi?",
										"Türkan Þoray","Filiz Akýn","Hülya Koçyiðit","Belgin Doruk",'a' );
		Questions q2 = new Questions("Elektirik akýmý ölçü birimi nedir?", "volt", "watt", "amper", 
							"ohm",'c');
		
		Questions q3 = new Questions("Tc anayasasýnýn ilk maddesi neyle ilgilidir?", "Devletin dini", 
									"Devletin þekli", "Devletin bayraðý", "Devletin dili", 'b');
		questions.add(q1);
		questions.add(q2);
		questions.add(q3);
		
		
	}


}