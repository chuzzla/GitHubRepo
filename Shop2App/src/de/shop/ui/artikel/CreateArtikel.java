package de.shop.ui.artikel;

import static de.shop.util.Constants.ARTIKEL_KEY;
import de.shop.R;
import de.shop.data.Artikel;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.TextView.OnEditorActionListener;

public class CreateArtikel extends Fragment implements OnClickListener {
	private static final String LOG_TAG = CreateArtikel.class.getSimpleName();
	
	private Artikel artikel;
	private EditText newBezeichnung;
	private EditText newPreis;
	private ToggleButton newVerfuegbar;
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		artikel = (Artikel) getArguments().get(ARTIKEL_KEY);
		Log.d(LOG_TAG, artikel.toString());
        
		// Voraussetzung fuer onOptionsItemSelected()
		setHasOptionsMenu(false);
		
		// attachToRoot = false, weil die Verwaltung des Fragments durch die Activity erfolgt
		return inflater.inflate(R.layout.create_artikel, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		//view.findViewById(R.id.btn_create_artikel).setOnClickListener(this);
		
    	newBezeichnung = (EditText) view.findViewById(R.id.bezeichnung_new);
    	newBezeichnung.setText("z.B. Hose");
    	
    	newPreis = (EditText) view.findViewById(R.id.preis_new);
    	newPreis.setText("z.B. 15.99"); 
    	
    	newVerfuegbar = (ToggleButton) view.findViewById(R.id.newsletter_tgl);
    	newVerfuegbar.setChecked(true);
    }
	
	private void setArtikel() {
		artikel.id = null;
		artikel.version = 0;
		artikel.bezeichnung = newBezeichnung.getText().toString();
		artikel.preis = Double.valueOf(newPreis.getText().toString());
		artikel.verfuegbar = newVerfuegbar.isChecked();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		
	}
}
