package de.shop.ui.artikel;

import static de.shop.util.Constants.ARTIKEL_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.shop.R;
import de.shop.data.Artikel;
import android.app.Fragment;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ArtikelListeNav extends ListFragment implements OnItemClickListener {
	private static final String LOG_TAG = ArtikelListeNav.class.getSimpleName();
	
	private static final String ID = "id";
	private static final String BEZEICHNUNG = "bezeichnung";
	private static final String[] FROM = {ID, BEZEICHNUNG};
	private static final int[] TO = {R.id.artikel_id, R.id.bezeichnung_txt};
	
	private List<Artikel> artikel;
	private List<Map<String, Object>> artikelItems;
	private int position = 0;

	@Override
	@SuppressWarnings("unchecked")
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        artikel = (List<Artikel>) getActivity().getIntent().getExtras().get(ARTIKEL_KEY);
        Log.d(LOG_TAG, artikel.toString());
        
		final ListAdapter listAdapter = createListAdapter();
        setListAdapter(listAdapter);
        
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	private ListAdapter createListAdapter() {
		artikelItems = new ArrayList<Map<String, Object>>(artikel.size());
		for (Artikel a : artikel) {
    		final Map<String, Object> artikelItem = new HashMap<String, Object>(2, 1); // max 2 Eintraege, bis zu 100 % Fuellung
    		artikelItem.put(ID, a.id);
    		artikelItem.put(BEZEICHNUNG, a.bezeichnung);
    		artikelItems.add(artikelItem);        	
        }
		
		final ListAdapter listAdapter = new SimpleAdapter(getActivity(), artikelItems, R.layout.artikel_liste_item, FROM, TO);
		return listAdapter;
    }
	
	public void refresh(Artikel artkl) {
		artikel.set(position, artkl);
		final ListAdapter listAdapter = createListAdapter();
        setListAdapter(listAdapter);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
        // Das Fragment (this) ist gleichzeitig der Listener, der auf Klicks in der Navigationsleiste reagiert
		// und implementiert deshalb die Methode onMenuItemClick()
        getListView().setOnItemClickListener(this);
	}

	@Override
	// Implementierung zum Interface OnItemClickListener fuer die Item-Liste
	public void onItemClick(AdapterView<?> adapterView, View view, int itemPosition, long itemId) {
		// view: TextView innerhalb von ListFragment
		// itemPosition: Textposition innerhalb der Liste mit Zaehlung ab 0
		// itemId = itemPosition bei String-Arrays bzw. = Primaerschluessel bei Listen aus einer DB
		
		Log.d(LOG_TAG, artikel.get(itemPosition).toString());
		
		// Evtl. vorhandene Tabs der ACTIVITY loeschen
    	getActivity().getActionBar().removeAllTabs();
		
    	// angeklickte Position fuer evtl. spaeteres Refresh merken, falls der angeklickte Kunde noch aktualisiert wird
    	position = itemPosition;
    	
		final Artikel artkl = artikel.get(itemPosition);
		final Bundle args = new Bundle(1);
		args.putSerializable(ARTIKEL_KEY, artkl);
		
		final Fragment neuesFragment = new ArtikelDetails();
		neuesFragment.setArguments(args);
		
		// Kein Name (null) fuer die Transaktion, da die Klasse BackStageEntry nicht verwendet wird
		getFragmentManager().beginTransaction()
		                    .replace(R.id.details, neuesFragment)
		                    .addToBackStack(null)  
		                    .commit();
	}

}
