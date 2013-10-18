package de.shop.service;

import static de.shop.ui.main.Prefs.mock;
import static de.shop.ui.main.Prefs.timeout;
import static de.shop.util.Constants.ARTIKEL_PATH;
import static de.shop.util.Constants.BEZEICHNUNG_PATH;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;


import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import de.shop.R;
import de.shop.data.Artikel;
import de.shop.util.InternalShopError;

public class ArtikelService extends Service {
	private static final String LOG_TAG = ArtikelService.class.getSimpleName();

	private final ArtikelServiceBinder binder = new ArtikelServiceBinder();

	public static Artikel getArtikel(String artikelUri) {
		HttpResponse<Artikel> artikel = WebServiceClient.getJsonSingle(artikelUri, Artikel.class);
		
		Log.d(LOG_TAG, artikel.resultObject.toString());
		return artikel.resultObject;
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	private ProgressDialog progressDialog;
	private ProgressDialog showProgressDialog(Context ctx) {
		progressDialog = new ProgressDialog(ctx);  // Objekt this der umschliessenden Klasse Startseite
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);  // Kreis (oder horizontale Linie)
		progressDialog.setMessage(getString(R.string.s_bitte_warten));
		progressDialog.setCancelable(true);      // Abbruch durch Zuruecktaste 
		progressDialog.setIndeterminate(true);   // Unbekannte Anzahl an Bytes werden vom Web Service geliefert
		progressDialog.show();
		return progressDialog;
	}
	
	public class ArtikelServiceBinder extends Binder {
		
		// Aufruf in einem eigenen Thread
		public HttpResponse<Artikel> sucheArtikelById(Long id, final Context ctx) {
			
			// (evtl. mehrere) Parameter vom Typ "Long", Resultat vom Typ "Artikel"
			final AsyncTask<Long, Void, HttpResponse<Artikel>> getArtikelByIdTask = new AsyncTask<Long, Void, HttpResponse<Artikel>>() {

				@Override
	    		protected void onPreExecute() {
					progressDialog = showProgressDialog(ctx);
				}
				
				@Override
				// Neuer Thread, damit der UI-Thread nicht blockiert wird
				protected HttpResponse<Artikel> doInBackground(Long... ids) {
					final Long artikelId = ids[0];
		    		final String path = ARTIKEL_PATH + "/" + artikelId;
		    		Log.v(LOG_TAG, "path = " + path);

		    		final HttpResponse<Artikel> result = mock
		    				                                ? Mock.sucheArtikelById(artikelId)
		    				                                : WebServiceClient.getJsonSingle(path, Artikel.class);
					Log.d(LOG_TAG + ".AsyncTask", "doInBackground: " + result);
					return result;
				}
				
				@Override
	    		protected void onPostExecute(HttpResponse<Artikel> unused) {
					progressDialog.dismiss();
	    		}
			};
			
			getArtikelByIdTask.execute(Long.valueOf(id));
			HttpResponse<Artikel> result = null;
	    	try {
	    		result = getArtikelByIdTask.get(timeout, SECONDS);
			}
	    	catch (Exception e) {
	    		throw new InternalShopError(e.getMessage(), e);
			}
	    	
	    	return result;
		}
		
		public HttpResponse<Artikel> sucheArtikelByBezeichnung(String bezeichnung, final Context ctx) {
			
			final AsyncTask<String, Void, HttpResponse<Artikel>> sucheArtikelByBezeichnungTask = new AsyncTask<String, Void, HttpResponse<Artikel>>() {
				@Override
	    		protected void onPreExecute() {
					progressDialog = showProgressDialog(ctx);
				}
				
				@Override
				// Neuer Thread, damit der UI-Thread nicht blockiert wird
				protected HttpResponse<Artikel> doInBackground(String... bezeichnungen) {
					final String bezeichnung = bezeichnungen[0];
					final String path = BEZEICHNUNG_PATH + bezeichnung;
					Log.v(LOG_TAG, "path = " + path);
		    		final HttpResponse<Artikel> result = WebServiceClient.getJsonList(path, Artikel.class);
					Log.d(LOG_TAG + ".AsyncTask", "doInBackground: " + result);
					return result;
				}
				
				@Override
	    		protected void onPostExecute(HttpResponse<Artikel> unused) {
					progressDialog.dismiss();
	    		}
			};
			
			sucheArtikelByBezeichnungTask.execute(bezeichnung);
			HttpResponse<Artikel> result = null;
			try {
				result = sucheArtikelByBezeichnungTask.get(timeout, SECONDS);
			}
	    	catch (Exception e) {
	    		throw new InternalShopError(e.getMessage(), e);
			}

	    	if (result.responseCode != HTTP_OK) {
	    		return result;
	    	}
	    	
	    	// URLs fuer Emulator anpassen
			return result;
		}
	}
}
