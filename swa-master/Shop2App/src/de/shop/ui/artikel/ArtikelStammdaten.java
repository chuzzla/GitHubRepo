package de.shop.ui.artikel;

import static de.shop.util.Constants.ARTIKEL_KEY;
import de.shop.ui.main.Prefs;
import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import de.shop.R;
import de.shop.data.Artikel;

public class ArtikelStammdaten extends Fragment {
	private static final String LOG_TAG = ArtikelStammdaten.class.getSimpleName();

	private Artikel artikel;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		artikel = (Artikel) getArguments().get(ARTIKEL_KEY);
		Log.d(LOG_TAG,artikel.toString());
		
		setHasOptionsMenu(true);
		
		return inflater.inflate(R.layout.artikel_stammdaten, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		fillValues(view);
    	
    	
	     // Context und OnGestureListener als Argumente
    }
	
	private void fillValues(View view) {
		final TextView txtId = (TextView) view.findViewById(R.id.artikel_id);
    	txtId.setText(artikel.id.toString());
    	
    	final TextView txtBezeichnung = (TextView) view.findViewById(R.id.bezeichnung_txt);
    	txtBezeichnung.setText(artikel.bezeichnung.toString());
    	
    	final TextView txtPreis = (TextView) view.findViewById(R.id.preis);
    	txtPreis.setText(artikel.preis.toString());
    	
    	final TextView txtVerfuegbar = (TextView) view.findViewById(R.id.verfuegbar);
    	txtVerfuegbar.setText(artikel.verfuegbar.toString());
   	}
	
	@Override
	// http://developer.android.com/guide/topics/ui/actionbar.html#ChoosingActionItems :
	// "As a general rule, all items in the options menu (let alone action items) should have a global impact on the app,
	//  rather than affect only a small portion of the interface."
	// Nur aufgerufen, falls setHasOptionsMenu(true) in onCreateView() aufgerufen wird
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.artikel_stammdaten_options, menu);
		
		// "Searchable Configuration" in res\xml\searchable.xml wird der SearchView zugeordnet
		final Activity activity = getActivity();
	    final SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
	    final SearchView searchView = (SearchView) menu.findItem(R.id.suchen).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.edit:
				// Evtl. vorhandene Tabs der ACTIVITY loeschen
		    	getActivity().getActionBar().removeAllTabs();
		    	
				final Bundle args = new Bundle(1);
				args.putSerializable(ARTIKEL_KEY, artikel);
				
				final Fragment neuesFragment = new ArtikelEdit();
				neuesFragment.setArguments(args);
				
				// Kein Name (null) fuer die Transaktion, da die Klasse BackStageEntry nicht verwendet wird
				getFragmentManager().beginTransaction()
				                    .replace(R.id.details, neuesFragment)
				                    .addToBackStack(null)  
				                    .commit();
				return true;
				
			case R.id.einstellungen:
				getFragmentManager().beginTransaction()
                                    .replace(R.id.details, new Prefs())
                                    .addToBackStack(null)
                                    .commit();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
