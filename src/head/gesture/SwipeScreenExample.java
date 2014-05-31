package head.gesture;

import head.gesture.SimpleGestureFilter.SimpleGestureListener;

import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

public class SwipeScreenExample extends Activity implements
		SimpleGestureListener {

	private SimpleGestureFilter detector;
	ArrayList<Questions> questions = new ArrayList<Questions>();
	Integer score = 0;
	Character answer;
	int nextQ;
	private Chronometer ch;
	private TextView score_text;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.swipe_screen);
		generateQuestions();
		
		detector = new SimpleGestureFilter(this, this);
		score_text = (TextView)findViewById(R.id.scoreText);
		score_text.setText("0");
		
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
			score_text.setText(score.toString());
			new AlertDialog.Builder(this).setTitle("Doðru").setMessage(score.toString()+" Puan!").setIcon(R.drawable.ok).setNeutralButton("Kapat", null).show();
			ch.stop();
			ch.setText("00:00");
		}
		else{ 
			new AlertDialog.Builder(this).setTitle("Üzgünüm").setMessage("Yanlýþ!").setIcon(R.drawable.wrong).setNeutralButton("Kapat", null).show();
			ch.stop();
			ch.setText("00:00");
		}
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
		ch.start();
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
		
		Questions q4 = new Questions("Hangisi 4 ayaklý deðildir? ", "Kedi", 
									"Tavþan", "Köpek", "Papaðan", 'd');
		Questions q5 = new Questions("Hangisi meyve deðildir? ", "Havuç", 
				"Elma", "Armut", "Portakal", 'a');
		Questions q6 = new Questions("Hangisi ana renklerden deðildir?", "sarý", "mavi", "pembe", "kýrmýzý", 'c');
		
		Questions q7 = new Questions("Ýngilizcede ördek?", "duck", "rabbit", "bird", "dog", 'a');
		
		questions.add(q1);
		questions.add(q2);
		questions.add(q3);
		questions.add(q4);
		questions.add(q5);
		questions.add(q6);
		questions.add(q7);
	}
}

