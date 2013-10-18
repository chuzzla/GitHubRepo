package de.shop.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import de.shop.R;
import de.shop.ui.bestellung.BestellungenActivity;
import de.shop.ui.kunde.KundenActivity;

public class MainSmartphone extends Activity implements OnClickListener {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
       	findViewById(R.id.btn_kunden).setOnClickListener(this);
       	findViewById(R.id.btn_bestellungen).setOnClickListener(this);
    }

	@Override
	// OnClickListener fuer die Buttons beim Dashboard fuer Smartphones
	public void onClick(View view) {
		Class<? extends Activity> nextActivity = null;
		switch (view.getId()) {
			case R.id.btn_kunden:
				nextActivity =  KundenActivity.class;
				break;
				
			case R.id.btn_bestellungen:
				nextActivity = BestellungenActivity.class;
				break;
				
			default:
				return;
		}
		
		final Intent intent = new Intent(view.getContext(), nextActivity);;
		startActivity(intent);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
