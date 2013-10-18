package de.shop.ui.artikel;

import static de.shop.util.Constants.ARTIKEL_KEY;

import java.util.ArrayList;

import de.shop.service.ArtikelService;
import de.shop.service.ArtikelService.ArtikelServiceBinder;
import de.shop.data.Artikel;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class SucheBezeichnungActivity extends Activity {
	private String LOG_TAG = SucheBezeichnungActivity.class.getSimpleName();
	
	private ArtikelServiceBinder artikelServiceBinder;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
			artikelServiceBinder = (ArtikelServiceBinder) serviceBinder;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			artikelServiceBinder = null;
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.search);

		final Intent intent = getIntent();
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
			return;
		}
		
		final String query = intent.getStringExtra(SearchManager.QUERY);
		Log.d(LOG_TAG, query);
		
		suchen(query);
	}
	
	@Override
	public void onResume() {
		final Intent intent = new Intent(this, ArtikelService.class);
		bindService(intent, serviceConnection, BIND_AUTO_CREATE);
		super.onResume();
	}

	@Override
	public void onPause() {
		unbindService(serviceConnection);
		super.onPause();
	}
	
	private void suchen(String name) {
		final ArrayList<? extends Artikel> artikel = artikelServiceBinder.sucheArtikelByBezeichnung(name, this).resultList;
		Log.d(LOG_TAG, artikel.toString());
		
		final Intent intent = new Intent(this, ArtikelListe.class);
		if (artikel != null && !artikel.isEmpty()) {
			intent.putExtra(ARTIKEL_KEY, artikel); 
		}
		startActivity(intent);
	}

}
