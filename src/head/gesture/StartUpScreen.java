package head.gesture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartUpScreen extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_activity);
		
		Button b =(Button) findViewById(R.id.button1);
		
		b.setOnClickListener(new View.OnClickListener() {
			@Override
            public void onClick(View v) {
				Intent in = new Intent(getApplicationContext(), SwipeScreenExample.class);
				startActivity(in);
			}
		});
	}
	
}
